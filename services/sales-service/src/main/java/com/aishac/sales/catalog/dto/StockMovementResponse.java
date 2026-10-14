package com.aishac.sales.catalog.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.aishac.sales.catalog.entity.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) UUID productId,

        @Schema(requiredMode = REQUIRED, ref = CatalogSchemas.STOCK_MOVEMENT_TYPE)
        StockMovementType type,

        @Schema(requiredMode = REQUIRED) int quantity,
        @Schema(requiredMode = REQUIRED) String reason,
        @Schema(requiredMode = REQUIRED, minimum = "0") int resultingStock,
        @Schema(requiredMode = REQUIRED) Instant createdAt) {}
