package com.aishac.customer.service;

import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CustomerErrorCode;
import java.util.regex.Pattern;

/** CPF: aceita com ou sem pontuação, valida os dígitos verificadores e devolve só os 11 dígitos (RN-02). */
public final class CpfValidator {

    private static final Pattern ACCEPTED = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");

    private CpfValidator() {}

    public static String normalize(String raw) {
        String trimmed = raw == null ? "" : raw.strip();
        if (!ACCEPTED.matcher(trimmed).matches()) {
            throw new BusinessException(CustomerErrorCode.INVALID_CPF);
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.chars().distinct().count() == 1 // 000.000.000-00, 111.111.111-11, ...
                || checkDigit(digits, 9) != digits.charAt(9) - '0'
                || checkDigit(digits, 10) != digits.charAt(10) - '0') {
            throw new BusinessException(CustomerErrorCode.INVALID_CPF);
        }
        return digits;
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
