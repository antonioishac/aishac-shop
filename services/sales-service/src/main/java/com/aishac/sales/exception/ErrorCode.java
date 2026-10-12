package com.aishac.sales.exception;

import org.springframework.http.HttpStatus;

/**
 * Item de um catálogo de erros. O {@code code()} vai na propriedade {@code code} do ProblemDetail: é o identificador
 * estável que o cliente da API usa para tratar o erro, independente do texto da mensagem (ADR-008).
 */
public interface ErrorCode {

    String code();

    String defaultMessage();

    HttpStatus httpStatus();
}
