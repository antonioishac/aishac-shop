package com.aishac.customer.dto;

import com.aishac.customer.entity.CustomerStatus;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String cpf,
        String email,
        String phone,
        CustomerStatus status,
        String blockReason,
        Instant blockedAt,
        Instant deactivatedAt,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
