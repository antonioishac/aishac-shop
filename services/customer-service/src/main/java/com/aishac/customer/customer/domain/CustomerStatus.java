package com.aishac.customer.customer.domain;

import java.util.Set;

/** Situação do cliente e transições permitidas (RN-06). INACTIVE é final na fase 01 (RN-07). */
public enum CustomerStatus {
    ACTIVE,
    BLOCKED,
    INACTIVE;

    public boolean canTransitionTo(CustomerStatus target) {
        return allowedTargets().contains(target);
    }

    private Set<CustomerStatus> allowedTargets() {
        return switch (this) {
            case ACTIVE -> Set.of(BLOCKED, INACTIVE);
            case BLOCKED -> Set.of(ACTIVE, INACTIVE);
            case INACTIVE -> Set.of();
        };
    }
}
