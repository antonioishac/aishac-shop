package com.aishac.customer.exception;

import com.aishac.customer.exception.ProblemDetailFactory.FieldError;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tradução central de exceções para ProblemDetail (RFC 9457). Estende {@link ResponseEntityExceptionHandler} para
 * herdar o tratamento dos erros de framework; todo caminho converge para {@link #handleExceptionInternal}, que
 * completa o corpo com {@code code}, {@code correlationId} e {@code timestamp} e loga no nível certo (WARN para erro
 * do cliente, ERROR com stack trace só para 5xx). Detalhe interno nunca vai na resposta.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;

    // ---- exceções do serviço --------------------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(ex.getErrorCode(), ex.getMessage());
        return handleExceptionInternal(
                ex, problem, new HttpHeaders(), ex.getErrorCode().httpStatus(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    // ---- banco ----------------------------------------------------------------------------------

    /**
     * Duas requisições alteraram o mesmo recurso ao mesmo tempo (lock otimista), ou a trava pessimista não pôde ser
     * obtida: o cliente deve recarregar e tentar de novo.
     */
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<Object> handleConcurrency(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(
                CommonErrorCode.CONCURRENT_MODIFICATION, CommonErrorCode.CONCURRENT_MODIFICATION.defaultMessage());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    /**
     * CHECK do banco violado (ex.: contagem de caracteres diferente entre Java e PostgreSQL) é dado inválido (400);
     * qualquer outra violação de integridade é conflito (409). UNIQUE de CPF/e-mail já chega traduzida pelo service.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        String constraint = ConstraintViolations.constraintName(ex).orElse("unknown");
        ProblemDetail problem = ConstraintViolations.isCheckConstraint(constraint)
                ? ProblemDetailFactory.validation(
                        List.of(new FieldError(ConstraintViolations.apiField(constraint), "violates a database rule")))
                : ProblemDetailFactory.build(
                        CommonErrorCode.DATA_INTEGRITY_VIOLATION,
                        CommonErrorCode.DATA_INTEGRITY_VIOLATION.defaultMessage());
        return handleExceptionInternal(
                ex, problem, new HttpHeaders(), HttpStatus.valueOf(problem.getStatus()), request);
    }

    /** {@code sort} com propriedade inexistente em endpoint paginado. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Object> handleInvalidSort(PropertyReferenceException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.validation(
                List.of(new FieldError("sort", "unknown property: " + ex.getPropertyName())));
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    /** Banco fora do ar ou sem conexão disponível: dependência indisponível (503), não erro interno. */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<Object> handleDatabaseUnavailable(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(
                CommonErrorCode.SERVICE_UNAVAILABLE, CommonErrorCode.SERVICE_UNAVAILABLE.defaultMessage());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(
                CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.defaultMessage());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // ---- overrides do ResponseEntityExceptionHandler ---------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), String.valueOf(error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(ex, ProblemDetailFactory.validation(errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldError(
                                String.valueOf(result.getMethodParameter().getParameterName()),
                                String.valueOf(error.getDefaultMessage()))))
                .toList();
        return handleExceptionInternal(
                ex, ProblemDetailFactory.validation(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.validation(
                List.of(new FieldError(String.valueOf(ex.getPropertyName()), "has an invalid value")));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(
                CommonErrorCode.MALFORMED_REQUEST, CommonErrorCode.MALFORMED_REQUEST.defaultMessage());
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(
                CommonErrorCode.ROUTE_NOT_FOUND, CommonErrorCode.ROUTE_NOT_FOUND.defaultMessage());
        return handleExceptionInternal(ex, problem, headers, HttpStatus.NOT_FOUND, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetailFactory.build(
                CommonErrorCode.METHOD_NOT_ALLOWED, CommonErrorCode.METHOD_NOT_ALLOWED.defaultMessage());
        return handleExceptionInternal(ex, problem, headers, HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    /**
     * Mensagem segura para log. Só as mensagens do próprio catálogo vão inteiras; as do framework e do banco podem
     * trazer o valor rejeitado (CPF mal formatado, nome, telefone) ou a linha inteira do SQL, então delas sai apenas
     * o nome dos campos com a constraint violada, o nome da constraint do banco ou o tipo da exceção.
     */
    private static String safeMessage(Exception ex) {
        return switch (ex) {
            case BusinessException business -> business.getMessage();
            case NotFoundException notFound -> notFound.getMessage();
            case DataIntegrityViolationException integrity ->
                "constraint " + ConstraintViolations.constraintName(integrity).orElse("unknown");
            case MethodArgumentNotValidException invalid ->
                invalid.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ":" + error.getCode())
                        .collect(Collectors.joining(", ", "fields [", "]"));
            case HandlerMethodValidationException invalid ->
                invalid.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(error -> result.getMethodParameter().getParameterName() + ":"
                                        + lastCode(error.getCodes())))
                        .collect(Collectors.joining(", ", "parameters [", "]"));
            default -> ex.getClass().getSimpleName();
        };
    }

    /** O código mais genérico da constraint (ex.: {@code Size}), sem objeto nem campo. */
    private static String lastCode(String[] codes) {
        return codes == null || codes.length == 0 ? "invalid" : codes[codes.length - 1];
    }

    // ---- funil único: rastreio garantido + log no nível certo --------------------------------------

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail detail
                ? detail
                : ProblemDetailFactory.build(
                        CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.defaultMessage());
        HttpStatus resolved = HttpStatus.resolve(status.value());
        ProblemDetailFactory.complete(
                problem, resolved != null ? resolved.name() : "HTTP_" + status.value(), clock.instant());
        Object code = problem.getProperties().get("code");
        if (status.is5xxServerError()) {
            log.error("Request failed with {}", code, ex);
        } else {
            log.warn("Request rejected with {}: {}", code, safeMessage(ex));
        }
        return super.handleExceptionInternal(ex, problem, headers, status, request);
    }
}
