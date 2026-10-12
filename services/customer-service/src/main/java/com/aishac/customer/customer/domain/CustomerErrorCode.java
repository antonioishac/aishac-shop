package com.aishac.customer.customer.domain;

import com.aishac.customer.shared.domain.ErrorCategory;
import com.aishac.customer.shared.domain.ErrorCode;

/** Catálogo de erros do contexto Clientes (spec F01-020, seção 5; F01-026). */
public enum CustomerErrorCode implements ErrorCode {
    VALIDATION_ERROR(ErrorCategory.VALIDATION),
    INVALID_CPF(ErrorCategory.VALIDATION),
    CUSTOMER_NOT_FOUND(ErrorCategory.NOT_FOUND),
    ADDRESS_NOT_FOUND(ErrorCategory.NOT_FOUND),
    CPF_ALREADY_EXISTS(ErrorCategory.CONFLICT),
    EMAIL_ALREADY_EXISTS(ErrorCategory.CONFLICT),
    INVALID_STATUS_TRANSITION(ErrorCategory.CONFLICT),
    CONCURRENT_MODIFICATION(ErrorCategory.CONFLICT),
    CUSTOMER_INACTIVE(ErrorCategory.BUSINESS_RULE),
    ADDRESS_LIMIT_EXCEEDED(ErrorCategory.BUSINESS_RULE);

    private final ErrorCategory category;

    CustomerErrorCode(ErrorCategory category) {
        this.category = category;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}
