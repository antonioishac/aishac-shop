package com.aishac.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Endereço de entrega. CEP aceito com ou sem hífen; chega ao service só com os 8 dígitos. */
public record AddressRequest(
        @Schema(example = "Casa") @NotBlank @Size(max = 40) String label,

        @Schema(example = "01310-100") @NotBlank @Pattern(regexp = "\\d{8}", message = "must have 8 digits")
        String zipCode,

        @Schema(example = "Avenida Paulista") @NotBlank @Size(max = 150)
        String street,

        @Schema(example = "1000", description = "Aceita 'S/N'") @NotBlank @Size(max = 10)
        String number,

        @Schema(example = "Apto 42") @Size(max = 60) String complement,

        @Schema(example = "Bela Vista") @NotBlank @Size(max = 80)
        String district,

        @Schema(example = "São Paulo") @NotBlank @Size(max = 80)
        String city,

        @Schema(
                example = "SP",
                allowableValues = {
                    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR",
                    "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                })
        @NotBlank
        @Pattern(
                regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "must be a Brazilian state code")
        String state) {

    public AddressRequest {
        label = Inputs.strip(label);
        zipCode = zipCode == null ? null : zipCode.strip().replace("-", "");
        street = Inputs.strip(street);
        number = Inputs.strip(number);
        complement = Inputs.stripToNull(complement);
        district = Inputs.strip(district);
        city = Inputs.strip(city);
        state = state == null ? null : state.strip().toUpperCase(Locale.ROOT);
    }
}
