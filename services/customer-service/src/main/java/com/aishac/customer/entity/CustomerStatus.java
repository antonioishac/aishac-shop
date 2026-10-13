package com.aishac.customer.entity;

/** Situação do cliente. As transições permitidas (RN-06) são regra de negócio: ver {@code CustomerRules}. */
public enum CustomerStatus {
    ACTIVE,
    BLOCKED,
    INACTIVE
}
