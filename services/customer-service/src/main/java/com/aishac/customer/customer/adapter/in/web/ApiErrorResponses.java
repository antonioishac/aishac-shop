package com.aishac.customer.customer.adapter.in.web;

/** Descrições comuns das respostas de erro documentadas nos controllers (catálogo na spec F01-020). */
final class ApiErrorResponses {

    static final String PROBLEM_JSON = "application/problem+json";
    static final String BAD_REQUEST = "VALIDATION_ERROR (campos em errors[]) ou INVALID_CPF";
    static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    static final String NOT_FOUND = "CUSTOMER_NOT_FOUND ou ADDRESS_NOT_FOUND";
    static final String DUPLICATED = "CPF_ALREADY_EXISTS ou EMAIL_ALREADY_EXISTS";
    static final String UPDATE_CONFLICT = "EMAIL_ALREADY_EXISTS ou CONCURRENT_MODIFICATION";
    static final String TRANSITION_CONFLICT = "INVALID_STATUS_TRANSITION ou CONCURRENT_MODIFICATION";
    static final String INACTIVE = "CUSTOMER_INACTIVE";
    static final String ADDRESS_RULE = "CUSTOMER_INACTIVE ou ADDRESS_LIMIT_EXCEEDED";

    private ApiErrorResponses() {}
}
