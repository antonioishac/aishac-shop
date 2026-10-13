package com.aishac.customer.service;

import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CustomerErrorCode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Regras de cliente usadas pelos services de cadastro e de endereços. */
public final class CustomerRules {

    public static final int MAX_ADDRESSES = 10;

    /** Transições permitidas (RN-06). INACTIVE é final na fase 01 (RN-07). */
    private static final Map<CustomerStatus, Set<CustomerStatus>> TRANSITIONS = Map.of(
            CustomerStatus.ACTIVE, Set.of(CustomerStatus.BLOCKED, CustomerStatus.INACTIVE),
            CustomerStatus.BLOCKED, Set.of(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE),
            CustomerStatus.INACTIVE, Set.of());

    private CustomerRules() {}

    /** Cliente desativado não pode ter dados nem endereços alterados (RN-08). */
    public static void ensureNotInactive(Customer customer) {
        if (customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BusinessException(CustomerErrorCode.CUSTOMER_INACTIVE);
        }
    }

    /** Só as transições da RN-06 são aceitas. */
    public static void ensureCanTransition(Customer customer, CustomerStatus target) {
        if (!TRANSITIONS.get(customer.getStatus()).contains(target)) {
            throw new BusinessException(
                    CustomerErrorCode.INVALID_STATUS_TRANSITION,
                    "Customer status cannot change from " + customer.getStatus() + " to " + target + ".");
        }
    }

    /** CEP guardado só com os 8 dígitos (RN-11). */
    public static String normalizeZipCode(String zipCode) {
        return zipCode.replace("-", "");
    }

    /** E-mail guardado sem espaços e em minúsculas: a unicidade não diferencia maiúsculas (RN-04). */
    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
