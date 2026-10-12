package com.aishac.sales.exception;

import com.aishac.sales.config.CorrelationIdFilter;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Corpo de erro padrão da API: ProblemDetail (RFC 9457) com {@code code} (do catálogo), {@code correlationId} (o
 * mesmo do log) e {@code timestamp}. Erros de validação trazem também {@code errors[]}.
 */
public final class ProblemDetailFactory {

    private ProblemDetailFactory() {}

    public record FieldError(String field, String message) {}

    public static ProblemDetail build(ErrorCode errorCode, String detail) {
        return build(errorCode.httpStatus(), errorCode, detail);
    }

    public static ProblemDetail build(HttpStatus status, ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", errorCode.code());
        return problem;
    }

    public static ProblemDetail validation(List<FieldError> errors) {
        ProblemDetail problem =
                build(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.defaultMessage());
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Completa qualquer ProblemDetail (inclusive os criados pelo próprio Spring) com rastreio e código. */
    public static ProblemDetail complete(ProblemDetail problem, String fallbackCode, Instant now) {
        if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
            problem.setProperty("code", fallbackCode);
        }
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        problem.setProperty("timestamp", now);
        return problem;
    }
}
