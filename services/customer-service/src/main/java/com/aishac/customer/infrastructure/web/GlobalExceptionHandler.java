package com.aishac.customer.infrastructure.web;

import com.aishac.customer.customer.domain.CustomerErrorCode;
import com.aishac.customer.shared.domain.DomainException;
import com.aishac.customer.shared.domain.ErrorCategory;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Toda resposta de erro sai em ProblemDetail (RFC 9457) com {@code code}, {@code correlationId} e
 * {@code timestamp} (ADR-008). Exceções de domínio viram status pela categoria do código do catálogo.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final Map<ErrorCategory, HttpStatus> STATUS_BY_CATEGORY = Map.of(
            ErrorCategory.VALIDATION, HttpStatus.BAD_REQUEST,
            ErrorCategory.NOT_FOUND, HttpStatus.NOT_FOUND,
            ErrorCategory.CONFLICT, HttpStatus.CONFLICT,
            ErrorCategory.BUSINESS_RULE, HttpStatus.UNPROCESSABLE_CONTENT);

    private final Clock clock;

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> handleDomain(DomainException exception, WebRequest request) {
        HttpStatus status = STATUS_BY_CATEGORY.get(exception.errorCode().category());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setProperty("code", exception.errorCode().code());
        if (exception.field() != null) {
            problem.setProperty("errors", List.of(fieldError(exception.field(), exception.getMessage())));
        }
        return handleExceptionInternal(exception, problem, new HttpHeaders(), status, request);
    }

    /** Duas transações alteraram o mesmo agregado: perde quem gravou por último (lock otimista). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException exception, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Resource was changed by another request. Reload and try again.");
        problem.setProperty("code", CustomerErrorCode.CONCURRENT_MODIFICATION.code());
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unexpected error", exception);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setProperty("code", INTERNAL_ERROR);
        return handleExceptionInternal(
                exception, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /** Corpo com campos inválidos (Bean Validation nos Records de request). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = validationProblem(exception.getBody());
        problem.setProperty(
                "errors",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> fieldError(error.getField(), error.getDefaultMessage()))
                        .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    /** Parâmetros de query, path ou header inválidos (ex.: size > 100, sort desconhecido). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = validationProblem(exception.getBody());
        problem.setProperty(
                "errors",
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(error -> fieldError(
                                        String.valueOf(
                                                result.getMethodParameter().getParameterName()),
                                        message(error))))
                        .toList());
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Valor que não converte para o tipo esperado (ex.: UUID mal formado, status inexistente). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request has invalid fields.");
        problem.setProperty("code", VALIDATION_ERROR);
        problem.setProperty(
                "errors", List.of(fieldError(String.valueOf(exception.getPropertyName()), "has an invalid value")));
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(exception, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            enrich(problem, status);
        }
        return response;
    }

    private static ProblemDetail validationProblem(ProblemDetail problem) {
        problem.setDetail("Request has invalid fields.");
        problem.setProperty("code", VALIDATION_ERROR);
        return problem;
    }

    private static Map<String, String> fieldError(String field, @Nullable String message) {
        return Map.of("field", field, "message", String.valueOf(message));
    }

    private static String message(MessageSourceResolvable error) {
        return String.valueOf(error.getDefaultMessage());
    }

    private void enrich(ProblemDetail problem, HttpStatusCode status) {
        Map<String, @Nullable Object> properties = problem.getProperties();
        if (properties == null || !properties.containsKey("code")) {
            problem.setProperty("code", defaultCode(status));
        }
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        problem.setProperty("timestamp", clock.instant());
    }

    private static String defaultCode(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.name() : "HTTP_" + status.value();
    }
}
