package com.aishac.customer.customer.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record AddressRequest(
        @NotBlank @Size(max = 40) @Schema(example = "Casa") String label,

        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "must have 8 digits") @Schema(example = "01310-100")
        String zipCode,

        @NotBlank @Size(max = 150) @Schema(example = "Avenida Paulista")
        String street,

        @NotBlank @Size(max = 10) @Schema(example = "1000") String number,

        @Size(max = 60) @Schema(example = "Apto 42", nullable = true) @Nullable
        String complement,

        @NotBlank @Size(max = 80) @Schema(example = "Bela Vista")
        String district,

        @NotBlank @Size(max = 80) @Schema(example = "São Paulo")
        String city,

        @NotBlank
        @Pattern(
                regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "must be a Brazilian state code")
        @Schema(example = "SP")
        String state) {

    public AddressRequest {
        label = Strings.strip(label);
        zipCode = Strings.strip(zipCode);
        street = Strings.strip(street);
        number = Strings.strip(number);
        complement = Strings.stripToNull(complement);
        district = Strings.strip(district);
        city = Strings.strip(city);
        state = Strings.strip(state);
    }
}
