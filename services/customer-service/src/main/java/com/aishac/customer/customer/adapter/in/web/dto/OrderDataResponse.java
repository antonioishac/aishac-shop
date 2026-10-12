package com.aishac.customer.customer.adapter.in.web.dto;

import com.aishac.customer.customer.domain.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record OrderDataResponse(
        UUID customerId,
        String fullName,
        String email,
        CustomerStatus status,

        @Schema(nullable = true, description = "Nulo se não houver addressId nem endereço padrão") @Nullable
        AddressResponse shippingAddress) {}
