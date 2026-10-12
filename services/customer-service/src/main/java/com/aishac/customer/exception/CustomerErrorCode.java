package com.aishac.customer.exception;

import org.springframework.http.HttpStatus;

/** Catálogo de erros de clientes (spec F01-020, seção 5; F01-026). */
public enum CustomerErrorCode implements ErrorCode {
    INVALID_CPF("CPF is invalid.", HttpStatus.BAD_REQUEST),
    CUSTOMER_NOT_FOUND("Customer not found.", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND("Address not found.", HttpStatus.NOT_FOUND),
    CPF_ALREADY_EXISTS("CPF is already registered.", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("E-mail is already registered.", HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION("Customer status cannot change this way.", HttpStatus.CONFLICT),
    CUSTOMER_INACTIVE("Customer is inactive and cannot be changed.", HttpStatus.UNPROCESSABLE_CONTENT),
    ADDRESS_LIMIT_EXCEEDED("Customer cannot have more than 10 addresses.", HttpStatus.UNPROCESSABLE_CONTENT);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    CustomerErrorCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
