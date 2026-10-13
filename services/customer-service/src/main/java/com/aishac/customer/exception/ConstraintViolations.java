package com.aishac.customer.exception;

import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Nome da constraint do banco violada, extraído pelo Hibernate (não pelo texto da mensagem, que traz SQL e dados da
 * linha e não deve ir para logs nem respostas).
 */
public final class ConstraintViolations {

    private ConstraintViolations() {}

    public static Optional<String> constraintName(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return Optional.of(violation.getConstraintName());
            }
        }
        return Optional.empty();
    }

    public static boolean isCheckConstraint(String constraintName) {
        return constraintName.startsWith("ck_");
    }

    /**
     * Campo da API correspondente a um CHECK do banco, para a resposta não expor o nome da constraint. CHECK que
     * envolve mais de um campo (ex.: status e datas) responde como {@code request}.
     */
    public static String apiField(String constraintName) {
        return switch (constraintName) {
            case "ck_customers_full_name_length" -> "fullName";
            case "ck_customers_cpf_format" -> "cpf";
            case "ck_customers_email_normalized" -> "email";
            case "ck_customers_phone_format" -> "phone";
            case "ck_customer_addresses_label_not_blank" -> "label";
            case "ck_customer_addresses_zip_code_format" -> "zipCode";
            case "ck_customer_addresses_state" -> "state";
            case "ck_customer_addresses_country" -> "country";
            default -> "request";
        };
    }
}
