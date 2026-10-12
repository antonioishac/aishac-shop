package com.aishac.customer.customer.adapter.in.web.dto;

import com.aishac.customer.customer.domain.CustomerStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CustomerResponse(
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
