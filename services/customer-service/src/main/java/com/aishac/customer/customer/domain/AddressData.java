package com.aishac.customer.customer.domain;

import org.jspecify.annotations.Nullable;

/** Dados editáveis de um endereço, já normalizados (CEP só com dígitos, complemento vazio vira ausente). */
public record AddressData(
        String label,
        String zipCode,
        String street,
        String number,
        @Nullable String complement,
        String district,
        String city,
        BrazilianState state) {

    public AddressData {
        label = required("label", label, 40);
        zipCode = zipCode.replace("-", "").strip();
        if (!zipCode.matches("\\d{8}")) {
            throw CustomerException.invalidField("zipCode", "must have 8 digits");
        }
        street = required("street", street, 150);
        number = required("number", number, 10);
        complement = complement == null || complement.isBlank() ? null : limit("complement", complement.strip(), 60);
        district = required("district", district, 80);
        city = required("city", city, 80);
    }

    private static String required(String field, String value, int maxLength) {
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw CustomerException.invalidField(field, "must not be blank");
        }
        return limit(field, stripped, maxLength);
    }

    private static String limit(String field, String value, int maxLength) {
        if (value.length() > maxLength) {
            throw CustomerException.invalidField(field, "must have at most " + maxLength + " characters");
        }
        return value;
    }
}
