package com.aishac.sales.exception;

import org.springframework.http.HttpStatus;

/** Erros transversais a todo o serviço. */
public enum CommonErrorCode implements ErrorCode {
    VALIDATION_ERROR("Request has invalid fields.", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("Request body is malformed.", HttpStatus.BAD_REQUEST),
    ROUTE_NOT_FOUND("Route not found.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("HTTP method not supported by this route.", HttpStatus.METHOD_NOT_ALLOWED),
    CONCURRENT_MODIFICATION("Resource was changed by another request. Reload and try again.", HttpStatus.CONFLICT),
    DATA_INTEGRITY_VIOLATION("Request conflicts with existing data.", HttpStatus.CONFLICT),
    INTERNAL_ERROR("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    CommonErrorCode(String defaultMessage, HttpStatus httpStatus) {
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
