package com.aishac.sales.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank @Size(min = 2, max = 80) String name,
        @Schema(nullable = true) @Size(max = 500) String description,

        @Schema(description = "Versão lida antes da alteração (lock otimista)") @NotNull @PositiveOrZero
        Long version) {

    public UpdateCategoryRequest {
        name = Inputs.strip(name);
        description = Inputs.stripToNull(description);
    }
}
