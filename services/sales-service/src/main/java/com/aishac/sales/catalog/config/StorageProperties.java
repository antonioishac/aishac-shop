package com.aishac.sales.catalog.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Armazenamento das imagens de produto (rule storage, plano F01-030). Valores por perfil no config-repo.
 *
 * @param endpoint       vazio = AWS real; preenchido = LocalStack
 * @param publicEndpoint endpoint usado nas URLs pré-assinadas (o navegador acessa por ele); vazio = {@code endpoint}
 */
@Validated
@ConfigurationProperties(prefix = "aishac.storage")
public record StorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        URI endpoint,
        URI publicEndpoint,
        boolean pathStyleAccess,
        @NotNull Duration presignedUrlTtl,
        @NotNull DataSize maxFileSize,
        @Min(1) @Max(8) int maxImagesPerProduct) {

    public URI presignEndpoint() {
        return publicEndpoint != null ? publicEndpoint : endpoint;
    }
}
