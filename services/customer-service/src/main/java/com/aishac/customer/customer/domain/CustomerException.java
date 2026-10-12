package com.aishac.customer.customer.domain;

import com.aishac.customer.shared.domain.DomainException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Exceções do contexto Clientes, uma por código do catálogo, criadas pelas fábricas abaixo. */
public final class CustomerException extends DomainException {

    private CustomerException(CustomerErrorCode code, String message, @Nullable String field) {
        super(code, message, field);
    }

    public static CustomerException invalidField(String field, String message) {
        return new CustomerException(CustomerErrorCode.VALIDATION_ERROR, message, field);
    }

    public static CustomerException invalidCpf() {
        return new CustomerException(CustomerErrorCode.INVALID_CPF, "CPF is invalid.", "cpf");
    }

    public static CustomerException customerNotFound(UUID customerId) {
        return new CustomerException(CustomerErrorCode.CUSTOMER_NOT_FOUND, "Customer not found: " + customerId, null);
    }

    public static CustomerException addressNotFound(UUID addressId) {
        return new CustomerException(CustomerErrorCode.ADDRESS_NOT_FOUND, "Address not found: " + addressId, null);
    }

    public static CustomerException cpfAlreadyExists() {
        return new CustomerException(CustomerErrorCode.CPF_ALREADY_EXISTS, "CPF is already registered.", null);
    }

    public static CustomerException emailAlreadyExists() {
        return new CustomerException(CustomerErrorCode.EMAIL_ALREADY_EXISTS, "E-mail is already registered.", null);
    }

    public static CustomerException invalidStatusTransition(CustomerStatus from, CustomerStatus to) {
        return new CustomerException(
                CustomerErrorCode.INVALID_STATUS_TRANSITION,
                "Customer status cannot change from " + from + " to " + to + ".",
                null);
    }

    public static CustomerException concurrentModification() {
        return new CustomerException(
                CustomerErrorCode.CONCURRENT_MODIFICATION,
                "Customer was changed by another request. Reload and try again.",
                null);
    }

    public static CustomerException customerInactive() {
        return new CustomerException(
                CustomerErrorCode.CUSTOMER_INACTIVE, "Customer is inactive and cannot be changed.", null);
    }

    public static CustomerException addressLimitExceeded(int limit) {
        return new CustomerException(
                CustomerErrorCode.ADDRESS_LIMIT_EXCEEDED,
                "Customer cannot have more than " + limit + " addresses.",
                null);
    }
}
