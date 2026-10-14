package com.aishac.sales.catalog.service.impl;

import com.aishac.sales.catalog.config.StorageProperties;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.service.ProductImageStorage;
import com.aishac.sales.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Imagens no S3 (LocalStack no perfil {@code local}). O retry e os timeouts são do próprio AWS SDK
 * ({@code StorageConfiguration}); o Circuit Breaker {@code s3} corta as chamadas quando o S3 está fora, e qualquer
 * falha vira {@code IMAGE_STORAGE_UNAVAILABLE} (RN-22).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class S3ProductImageStorage implements ProductImageStorage {

    private static final String CIRCUIT = "s3";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    @Override
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "putUnavailable")
    public void put(String objectKey, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .contentLength((long) content.length)
                        .build(),
                RequestBody.fromBytes(content));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "deleteUnavailable")
    public void delete(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build());
    }

    @Override
    public PresignedUrl presign(String objectKey) {
        try {
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(properties.presignedUrlTtl())
                    .getObjectRequest(
                            request -> request.bucket(properties.bucket()).key(objectKey))
                    .build());
            return new PresignedUrl(presigned.url().toURI(), presigned.expiration());
        } catch (SdkException | URISyntaxException e) {
            throw unavailable(e);
        }
    }

    @SuppressWarnings("unused") // fallback do Circuit Breaker
    private void putUnavailable(String objectKey, byte[] content, String contentType, Throwable cause) {
        throw unavailable(cause);
    }

    @SuppressWarnings("unused") // fallback do Circuit Breaker
    private void deleteUnavailable(String objectKey, Throwable cause) {
        throw unavailable(cause);
    }

    private static BusinessException unavailable(Throwable cause) {
        log.warn("Image storage call failed: {}", cause.getClass().getSimpleName());
        return new BusinessException(CatalogErrorCode.IMAGE_STORAGE_UNAVAILABLE);
    }
}
