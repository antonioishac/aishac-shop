package com.aishac.customer.shared.domain;

/** Item de um catálogo de erros: código estável exposto na API e sua categoria. */
public interface ErrorCode {

    String code();

    ErrorCategory category();
}
