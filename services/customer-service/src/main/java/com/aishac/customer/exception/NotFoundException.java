package com.aishac.customer.exception;

import lombok.Getter;

/** Recurso não encontrado pelo id. Sempre 404, com o código específico do recurso. */
@Getter
public class NotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public NotFoundException(ErrorCode errorCode, Object id) {
        super(errorCode.defaultMessage() + " Id: " + id);
        this.errorCode = errorCode;
    }
}
