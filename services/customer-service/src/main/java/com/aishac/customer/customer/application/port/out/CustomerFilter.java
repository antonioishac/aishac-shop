package com.aishac.customer.customer.application.port.out;

import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.CustomerStatus;
import com.aishac.customer.customer.domain.Email;
import org.jspecify.annotations.Nullable;

/**
 * Filtros combináveis da listagem. {@code name} é "contém, sem diferenciar maiúsculas"; CPF e e-mail são
 * exatos e já normalizados (CA-07, CA-08).
 */
public record CustomerFilter(
        @Nullable String name,
        @Nullable Email email,
        @Nullable Cpf cpf,
        @Nullable CustomerStatus status) {

    public CustomerFilter {
        name = name == null || name.isBlank() ? null : name.strip();
    }
}
