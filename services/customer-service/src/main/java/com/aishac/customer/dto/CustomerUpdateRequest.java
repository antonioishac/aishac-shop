package com.aishac.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Atualização de nome, e-mail e telefone. O CPF não muda. {@code version} é a lida antes (lock otimista). */
@Schema(name = "UpdateCustomerRequest")
public record CustomerUpdateRequest(
        @NotBlank @Size(min = 3, max = 150) String fullName,

        @NotBlank
        @Email
        @Pattern(regexp = "\\p{ASCII}+", message = "must contain only ASCII characters")
        @Size(max = 254)
        String email,

        @Pattern(regexp = "\\d{10,11}", message = "must have 10 or 11 digits including area code")
        String phone,

        @Schema(description = "Versão lida antes da alteração") @NotNull @PositiveOrZero
        Long version) {

    public CustomerUpdateRequest {
        fullName = Inputs.strip(fullName);
        email = Inputs.strip(email);
        phone = Inputs.stripToNull(phone);
    }
}
