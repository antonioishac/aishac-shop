package com.aishac.customer.customer.application.port.out;

import com.aishac.customer.customer.domain.CustomerStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Modelo de leitura do cliente, sem endereços (listagem e detalhe). */
public record CustomerView(
        UUID id,
        String fullName,
        String cpf,
        String email,
        @Nullable String phone,
        CustomerStatus status,
        @Nullable String blockReason,
        @Nullable Instant blockedAt,
        @Nullable Instant deactivatedAt,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
