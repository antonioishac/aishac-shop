package com.aishac.customer.customer.adapter.in.web.dto;

import org.jspecify.annotations.Nullable;

/** Normalização de entrada antes da Bean Validation: espaços nas pontas não invalidam o campo. */
final class Strings {

    private Strings() {}

    static @Nullable String strip(@Nullable String value) {
        return value == null ? null : value.strip();
    }

    static @Nullable String stripToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
