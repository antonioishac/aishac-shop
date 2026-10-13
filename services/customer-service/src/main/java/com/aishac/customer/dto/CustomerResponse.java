package com.aishac.customer.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.aishac.customer.entity.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String fullName,

        @Schema(requiredMode = REQUIRED, description = "11 dígitos", example = "52998224725")
        String cpf,

        @Schema(requiredMode = REQUIRED, format = "email") String email,
        @Schema(nullable = true) String phone,

        @Schema(requiredMode = REQUIRED, ref = OpenApiSchemas.CUSTOMER_STATUS)
        CustomerStatus status,

        @Schema(nullable = true) String blockReason,
        @Schema(nullable = true) Instant blockedAt,
        @Schema(nullable = true) Instant deactivatedAt,
        @Schema(requiredMode = REQUIRED) long version,
        @Schema(requiredMode = REQUIRED) Instant createdAt,
        @Schema(requiredMode = REQUIRED) Instant updatedAt) {}
