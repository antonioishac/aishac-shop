package com.aishac.customer.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record AddressResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String label,

        @Schema(requiredMode = REQUIRED, description = "8 dígitos", example = "01310100")
        String zipCode,

        @Schema(requiredMode = REQUIRED) String street,
        @Schema(requiredMode = REQUIRED) String number,
        @Schema(nullable = true) String complement,
        @Schema(requiredMode = REQUIRED) String district,
        @Schema(requiredMode = REQUIRED) String city,

        @Schema(requiredMode = REQUIRED, ref = OpenApiSchemas.BRAZILIAN_STATE)
        String state,

        @Schema(requiredMode = REQUIRED, allowableValues = "BR")
        String country,

        @Schema(requiredMode = REQUIRED) boolean isDefault) {}
