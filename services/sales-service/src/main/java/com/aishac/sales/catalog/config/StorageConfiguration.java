package com.aishac.sales.catalog.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Clientes do S3 com a cadeia padrão de credenciais do AWS SDK (nunca chaves no código ou no config-repo). O retry
 * é o padrão do SDK; os timeouts abaixo limitam cada chamada (o Circuit Breaker fica na implementação do storage).
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        // Por tentativa, folgado para um upload de 5 MB em rede lenta; no total, com o retry do SDK.
                        .apiCallAttemptTimeout(Duration.ofSeconds(10))
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .build());
        if (properties.endpoint() != null) {
            builder.endpointOverride(properties.endpoint());
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(StorageProperties properties) {
        var builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build());
        if (properties.presignEndpoint() != null) {
            builder.endpointOverride(properties.presignEndpoint());
        }
        return builder.build();
    }
}
