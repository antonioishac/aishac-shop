package com.aishac.customer.dto;

/** Normalização de entrada antes da Bean Validation: espaços nas pontas não invalidam um campo. */
final class Inputs {

    private Inputs() {}

    static String strip(String value) {
        return value == null ? null : value.strip();
    }

    static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
