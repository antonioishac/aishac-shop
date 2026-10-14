package com.aishac.sales.catalog.dto;

import com.aishac.sales.catalog.entity.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(
        @Schema(ref = CatalogSchemas.STOCK_MOVEMENT_TYPE) @NotNull
        StockMovementType type,

        @Schema(example = "5") @NotNull @Min(1) @Max(100000) Integer quantity,

        @Schema(example = "Reposição do fornecedor") @NotBlank @Size(min = 3, max = 255)
        String reason) {

    public StockMovementRequest {
        reason = Inputs.strip(reason);
    }
}
