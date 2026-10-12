package com.aishac.customer.customer.domain;

import java.util.regex.Pattern;

/** CPF validado pelos dígitos verificadores, guardado só com os 11 dígitos (RN-02). */
public record Cpf(String value) {

    private static final Pattern ACCEPTED = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");

    public Cpf {
        if (!value.matches("\\d{11}") || !hasValidCheckDigits(value)) {
            throw CustomerException.invalidCpf();
        }
    }

    /** Aceita o CPF com ou sem pontuação (`529.982.247-25` ou `52998224725`). */
    public static Cpf of(String raw) {
        String trimmed = raw.strip();
        if (!ACCEPTED.matcher(trimmed).matches()) {
            throw CustomerException.invalidCpf();
        }
        return new Cpf(trimmed.replaceAll("\\D", ""));
    }

    private static boolean hasValidCheckDigits(String digits) {
        if (digits.chars().distinct().count() == 1) {
            return false; // 000.000.000-00, 111.111.111-11, ...
        }
        return checkDigit(digits, 9) == digits.charAt(9) - '0' && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
