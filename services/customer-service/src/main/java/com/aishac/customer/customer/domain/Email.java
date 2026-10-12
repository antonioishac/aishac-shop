package com.aishac.customer.customer.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** E-mail normalizado: sem espaços nas pontas e em minúsculas, o que torna a unicidade case-insensitive (RN-04). */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public Email {
        if (value.length() > MAX_LENGTH
                || !FORMAT.matcher(value).matches()
                || !value.equals(value.strip().toLowerCase(Locale.ROOT))) {
            throw CustomerException.invalidField("email", "must be a well-formed e-mail address");
        }
    }

    public static Email of(String raw) {
        return new Email(raw.strip().toLowerCase(Locale.ROOT));
    }
}
