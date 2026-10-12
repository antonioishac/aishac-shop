package com.aishac.customer.entity;

import java.util.Set;

/** Situação do cliente e transições permitidas (RN-06). INACTIVE é final na fase 01 (RN-07). */
public enum CustomerStatus {
    ACTIVE,
    BLOCKED,
    INACTIVE;

    public boolean canTransitionTo(CustomerStatus target) {
        return switch (this) {
            case ACTIVE -> Set.of(BLOCKED, INACTIVE).contains(target);
            case BLOCKED -> Set.of(ACTIVE, INACTIVE).contains(target);
            case INACTIVE -> false;
        };
    }
}
