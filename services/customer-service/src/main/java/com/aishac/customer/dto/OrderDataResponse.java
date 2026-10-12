package com.aishac.customer.dto;

import com.aishac.customer.entity.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Dados do cliente para criação de pedido no sales-service (RN-16, RN-17). */
public record OrderDataResponse(
        UUID customerId,
        String fullName,
        String email,
        CustomerStatus status,

        @Schema(nullable = true, description = "Nulo se não houver addressId nem endereço padrão")
        AddressResponse shippingAddress) {}
