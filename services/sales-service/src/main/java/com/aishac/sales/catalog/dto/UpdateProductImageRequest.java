package com.aishac.sales.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Metadados da imagem. Campo ausente ou nulo não muda; {@code primary} só aceita {@code true}. */
@Schema(description = "Pelo menos um campo. Ausente ou nulo não altera.", minProperties = 1)
public record UpdateProductImageRequest(
        @Size(min = 1, max = 150) String altText,
        @Min(1) @Max(8) Integer position,

        @Schema(description = "Só aceita true (a principal deixa de ser ao marcar outra)")
        @AssertTrue(message = "only accepts true")
        Boolean primary) {

    public UpdateProductImageRequest {
        altText = Inputs.strip(altText);
    }

    /** Aparece em {@code errors[]} como o campo {@code request}: a falha é do corpo inteiro, não de um campo. */
    @JsonIgnore
    @AssertTrue(message = "at least one of altText, position or primary must be informed")
    public boolean isRequest() {
        return altText != null || position != null || primary != null;
    }
}
