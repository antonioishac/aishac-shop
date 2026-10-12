package com.aishac.customer.exception;

import lombok.Getter;

/**
 * Violação de regra de negócio. Sempre carrega um {@link ErrorCode} do catálogo; o status HTTP vem dele (400, 409 ou
 * 422, conforme a regra).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
