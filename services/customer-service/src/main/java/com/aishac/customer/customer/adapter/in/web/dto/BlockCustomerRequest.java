package com.aishac.customer.customer.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockCustomerRequest(
        @NotBlank @Size(min = 3, max = 255) @Schema(example = "Suspeita de fraude")
        String reason) {

    public BlockCustomerRequest {
        reason = Strings.strip(reason);
    }
}
