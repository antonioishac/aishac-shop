package com.aishac.customer.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.aishac.customer.entity.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Dados do cliente para criação de pedido no sales-service (RN-16, RN-17). */
public record OrderDataResponse(
        @Schema(requiredMode = REQUIRED) UUID customerId,
        @Schema(requiredMode = REQUIRED) String fullName,
        @Schema(requiredMode = REQUIRED, format = "email") String email,

        @Schema(requiredMode = REQUIRED, ref = OpenApiSchemas.CUSTOMER_STATUS)
        CustomerStatus status,

        @Schema(
                requiredMode = REQUIRED,
                nullable = true,
                description = "Nulo se não houver addressId nem endereço padrão")
        AddressResponse shippingAddress) {}
