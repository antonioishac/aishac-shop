package com.aishac.sales.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Substitui nome, descrição, preço e categoria. SKU e estoque não mudam por aqui (RN-06, RN-10). */
public record UpdateProductRequest(
        @NotBlank @Size(min = 3, max = 150) String name,
        @Schema(nullable = true) @Size(max = 2000) String description,

        @Schema(ref = CatalogSchemas.MONEY)
        @NotBlank
        @Pattern(regexp = CatalogSchemas.MONEY_PATTERN, message = CatalogSchemas.MONEY_MESSAGE)
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        String price,

        @NotNull UUID categoryId,

        @Schema(description = "Versão lida antes da alteração (lock otimista)") @NotNull @PositiveOrZero
        Long version) {

    public UpdateProductRequest {
        name = Inputs.strip(name);
        description = Inputs.stripToNull(description);
        price = Inputs.strip(price);
    }
}
