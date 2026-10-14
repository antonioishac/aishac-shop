package com.aishac.sales.catalog.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        @Schema(requiredMode = REQUIRED) UUID id,

        @Schema(requiredMode = REQUIRED, example = "TENIS-RUN-42")
        String sku,

        @Schema(requiredMode = REQUIRED) String name,
        @Schema(nullable = true) String description,

        @Schema(requiredMode = REQUIRED, example = "299.90", description = "Sempre com 2 casas decimais")
        String price,

        @Schema(requiredMode = REQUIRED) CategorySummary category,
        @Schema(requiredMode = REQUIRED, minimum = "0") int stockQuantity,
        @Schema(requiredMode = REQUIRED) boolean active,

        @Schema(requiredMode = REQUIRED, description = "Produto e categoria ativos")
        boolean sellable,

        @Schema(
                requiredMode = REQUIRED,
                nullable = true,
                description = "Nulo se não houver imagem ou se a URL pré-assinada não puder ser gerada")
        ProductImageResponse primaryImage,

        @Schema(requiredMode = REQUIRED) long version,
        @Schema(requiredMode = REQUIRED) Instant createdAt,
        @Schema(requiredMode = REQUIRED) Instant updatedAt) {}
