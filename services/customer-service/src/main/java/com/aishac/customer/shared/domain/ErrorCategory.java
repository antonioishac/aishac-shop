package com.aishac.customer.shared.domain;

/** Natureza do erro de domínio. A borda HTTP traduz cada categoria em um status (ADR-008). */
public enum ErrorCategory {
    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    BUSINESS_RULE
}
