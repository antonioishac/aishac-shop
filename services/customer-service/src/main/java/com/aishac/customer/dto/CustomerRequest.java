package com.aishac.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro de cliente. CPF com ou sem pontuação; dígitos verificadores são validados no service. */
public record CustomerRequest(
        @Schema(example = "Ana Souza") @NotBlank @Size(min = 3, max = 150)
        String fullName,

        @Schema(example = "529.982.247-25")
        @NotBlank
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "must be a CPF with or without punctuation")
        String cpf,

        @Schema(example = "ana@example.com")
        @NotBlank
        @Email
        @Pattern(regexp = "\\p{ASCII}+", message = "must contain only ASCII characters")
        @Size(max = 254)
        String email,

        @Schema(example = "11987654321", description = "DDD + número, só dígitos")
        @Pattern(regexp = "\\d{10,11}", message = "must have 10 or 11 digits including area code")
        String phone) {

    public CustomerRequest {
        fullName = Inputs.strip(fullName);
        cpf = Inputs.strip(cpf);
        email = Inputs.strip(email);
        phone = Inputs.stripToNull(phone);
    }
}
