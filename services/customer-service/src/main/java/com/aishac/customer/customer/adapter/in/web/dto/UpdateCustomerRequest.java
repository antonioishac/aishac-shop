package com.aishac.customer.customer.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateCustomerRequest(
        @NotBlank @Size(min = 3, max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,

        @Pattern(regexp = "\\d{10,11}", message = "must have 10 or 11 digits including area code")
        @Schema(nullable = true)
        @Nullable
        String phone,

        @NotNull @PositiveOrZero @Schema(description = "Versão lida (lock otimista)")
        Long version) {

    public UpdateCustomerRequest {
        fullName = Strings.strip(fullName);
        email = Strings.strip(email);
        phone = Strings.stripToNull(phone);
    }
}
