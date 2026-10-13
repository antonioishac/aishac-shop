package com.aishac.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Só documentação OpenAPI: a forma do ProblemDetail que a API devolve (RFC 9457 + {@code code}, {@code correlationId},
 * {@code timestamp} e {@code errors}, ADR-008). Quem monta o corpo de verdade é o {@code GlobalExceptionHandler}.
 */
@Schema(name = "ProblemDetail", description = "RFC 9457 com as extensões do projeto (ADR-008)")
public record ProblemResponse(
        @Schema(example = "about:blank") String type,
        @Schema(example = "Not Found") String title,
        @Schema(example = "404") int status,
        @Schema(example = "Customer not found.") String detail,
        String instance,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER_NOT_FOUND")
        String code,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String correlationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant timestamp,
        @Schema(description = "Só em VALIDATION_ERROR") List<FieldError> errors) {

    public record FieldError(String field, String message) {}
}
