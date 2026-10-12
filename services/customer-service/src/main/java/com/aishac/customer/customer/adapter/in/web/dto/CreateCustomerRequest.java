package com.aishac.customer.customer.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateCustomerRequest(
        @NotBlank @Size(min = 3, max = 150) @Schema(example = "Ana Souza")
        String fullName,

        @NotBlank
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "must be a CPF with or without punctuation")
        @Schema(example = "529.982.247-25")
        String cpf,

        @NotBlank @Email @Size(max = 254) @Schema(example = "ana@example.com")
        String email,

        @Pattern(regexp = "\\d{10,11}", message = "must have 10 or 11 digits including area code")
        @Schema(example = "11987654321", nullable = true)
        @Nullable
        String phone) {

    public CreateCustomerRequest {
        fullName = Strings.strip(fullName);
        cpf = Strings.strip(cpf);
        email = Strings.strip(email);
        phone = Strings.stripToNull(phone);
    }
}
