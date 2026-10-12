package com.aishac.customer.customer.domain;

import org.jspecify.annotations.Nullable;

/** Telefone brasileiro com DDD: 10 (fixo) ou 11 (celular) dígitos. */
public record Phone(String value) {

    public Phone {
        if (!value.matches("\\d{10,11}")) {
            throw CustomerException.invalidField("phone", "must have 10 or 11 digits including area code");
        }
    }

    /** Telefone é opcional: nulo ou em branco resulta em ausência. */
    public static @Nullable Phone ofNullable(@Nullable String raw) {
        return raw == null || raw.isBlank() ? null : new Phone(raw.strip());
    }
}
