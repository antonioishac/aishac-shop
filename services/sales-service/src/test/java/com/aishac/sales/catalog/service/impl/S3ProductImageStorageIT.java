package com.aishac.sales.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aishac.sales.catalog.TestImages;
import com.aishac.sales.catalog.config.StorageProperties;
import com.aishac.sales.catalog.service.ProductImageStorage.PresignedUrl;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.unit.DataSize;
import org.testcontainers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Adapter real do S3 contra o LocalStack (mesma imagem do compose). Só roda com {@code LOCALSTACK_AUTH_TOKEN} no
 * ambiente (a imagem exige token desde a 2026.03); sem ele, os fluxos de imagem ficam cobertos pelo armazenamento em
 * memória do {@code CatalogApiIT}.
 */
@EnabledIfEnvironmentVariable(named = "LOCALSTACK_AUTH_TOKEN", matches = ".+")
class S3ProductImageStorageIT {

    private static final String BUCKET = "aishac-product-images-it";

    private static LocalStackContainer localstack;
    private static S3Client s3Client;
    private static S3Presigner s3Presigner;
    private static S3ProductImageStorage storage;

    @BeforeAll
    static void startLocalStack() {
        localstack = new LocalStackContainer("localstack/localstack:2026.03.0")
                .withServices("s3")
                .withEnv("LOCALSTACK_AUTH_TOKEN", System.getenv("LOCALSTACK_AUTH_TOKEN"));
        localstack.start();
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
        var pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        Region region = Region.of(localstack.getRegion());
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();
        s3Presigner = S3Presigner.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();
        s3Client.createBucket(request -> request.bucket(BUCKET));
        storage = new S3ProductImageStorage(
                s3Client,
                s3Presigner,
                new StorageProperties(
                        BUCKET,
                        localstack.getRegion(),
                        localstack.getEndpoint(),
                        null,
                        true,
                        Duration.ofMinutes(15),
                        DataSize.ofMegabytes(5),
                        8));
    }

    @AfterAll
    static void stopLocalStack() {
        s3Presigner.close();
        s3Client.close();
        localstack.stop();
    }

    // CA-16: a URL pré-assinada baixa exatamente o arquivo enviado
    @Test
    void shouldStoreAndServeObjectThroughPresignedUrl() throws Exception {
        // Given
        String key = "products/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png";
        byte[] content = TestImages.png(2048);

        // When
        storage.put(key, content, "image/png");
        PresignedUrl presigned = storage.presign(key);

        // Then
        HttpResponse<byte[]> download = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(presigned.url()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).isEqualTo(content);
        assertThat(download.headers().firstValue("Content-Type")).contains("image/png");
        assertThat(presigned.expiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
    }

    // CA-19, CA-21: o objeto removido deixa de existir no bucket
    @Test
    void shouldDeleteObject() {
        String key = "products/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        storage.put(key, TestImages.jpeg(512), "image/jpeg");

        storage.delete(key);

        assertThatThrownBy(() ->
                        s3Client.headObject(request -> request.bucket(BUCKET).key(key)))
                .isInstanceOf(NoSuchKeyException.class);
    }
}
