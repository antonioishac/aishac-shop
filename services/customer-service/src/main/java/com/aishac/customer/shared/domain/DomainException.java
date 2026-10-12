package com.aishac.customer.shared.domain;

import org.jspecify.annotations.Nullable;

/** Base das exceções de domínio: carrega o código do catálogo e, em erros de validação, o campo. */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final @Nullable String field;

    protected DomainException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    protected DomainException(ErrorCode errorCode, String message, @Nullable String field) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public @Nullable String field() {
        return field;
    }
}
