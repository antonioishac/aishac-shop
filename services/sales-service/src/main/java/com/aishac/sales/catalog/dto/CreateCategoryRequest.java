package com.aishac.sales.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @Schema(example = "Calçados") @NotBlank @Size(min = 2, max = 80)
        String name,

        @Schema(nullable = true, example = "Tênis, sapatos e sandálias") @Size(max = 500)
        String description) {

    public CreateCategoryRequest {
        name = Inputs.strip(name);
        description = Inputs.stripToNull(description);
    }
}
