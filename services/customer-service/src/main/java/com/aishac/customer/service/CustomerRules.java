package com.aishac.customer.service;

import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CustomerErrorCode;
import java.util.Locale;

/** Regras de cliente usadas pelos services de cadastro e de endereços. */
public final class CustomerRules {

    public static final int MAX_ADDRESSES = 10;

    private CustomerRules() {}

    /** Cliente desativado não pode ter dados nem endereços alterados (RN-08). */
    public static void ensureNotInactive(Customer customer) {
        if (customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BusinessException(CustomerErrorCode.CUSTOMER_INACTIVE);
        }
    }

    /** Só as transições da RN-06 são aceitas. */
    public static void ensureCanTransition(Customer customer, CustomerStatus target) {
        if (!customer.getStatus().canTransitionTo(target)) {
            throw new BusinessException(
                    CustomerErrorCode.INVALID_STATUS_TRANSITION,
                    "Customer status cannot change from " + customer.getStatus() + " to " + target + ".");
        }
    }

    /** E-mail guardado sem espaços e em minúsculas: a unicidade não diferencia maiúsculas (RN-04). */
    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
