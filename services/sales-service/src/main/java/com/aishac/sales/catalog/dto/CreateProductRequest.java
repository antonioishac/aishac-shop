package com.aishac.sales.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Cadastro de produto. O SKU é guardado em maiúsculas pelo service (RN-06); o preço é string decimal (RN-07). */
public record CreateProductRequest(
        @Schema(example = "tenis-run-42", description = "Letras, dígitos e hífen; guardado em maiúsculas")
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9-]{3,40}", message = "must have 3 to 40 letters, digits or hyphens")
        String sku,

        @Schema(example = "Tênis Run") @NotBlank @Size(min = 3, max = 150)
        String name,

        @Schema(nullable = true) @Size(max = 2000) String description,

        @Schema(ref = CatalogSchemas.MONEY)
        @NotBlank
        @Pattern(regexp = CatalogSchemas.MONEY_PATTERN, message = CatalogSchemas.MONEY_MESSAGE)
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        String price,

        @NotNull UUID categoryId,

        @Schema(example = "10") @NotNull @PositiveOrZero @Max(100000)
        Integer initialStock) {

    public CreateProductRequest {
        sku = Inputs.strip(sku);
        name = Inputs.strip(name);
        description = Inputs.stripToNull(description);
        price = Inputs.strip(price);
    }
}
