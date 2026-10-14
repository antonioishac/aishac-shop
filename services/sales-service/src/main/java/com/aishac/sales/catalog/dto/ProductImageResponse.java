package com.aishac.sales.catalog.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String altText,
        @Schema(requiredMode = REQUIRED, minimum = "1") int position,
        @Schema(requiredMode = REQUIRED) boolean primary,

        @Schema(
                requiredMode = REQUIRED,
                allowableValues = {"image/jpeg", "image/png", "image/webp"})
        String contentType,

        @Schema(requiredMode = REQUIRED) long sizeBytes,

        @Schema(requiredMode = REQUIRED, description = "URL pré-assinada de leitura")
        URI url,

        @Schema(requiredMode = REQUIRED) Instant urlExpiresAt) {}
