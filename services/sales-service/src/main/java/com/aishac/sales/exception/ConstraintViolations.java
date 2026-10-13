package com.aishac.sales.exception;

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
}
