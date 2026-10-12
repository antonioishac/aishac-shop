package com.aishac.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockCustomerRequest(
        @Schema(example = "Suspeita de fraude") @NotBlank @Size(min = 3, max = 255)
        String reason) {

    public BlockCustomerRequest {
        reason = Inputs.strip(reason);
    }
}
