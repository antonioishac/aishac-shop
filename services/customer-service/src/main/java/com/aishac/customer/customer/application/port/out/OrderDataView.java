package com.aishac.customer.customer.application.port.out;

import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.CustomerStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Dados do cliente para criação de pedido no sales-service (RN-16, RN-17). */
public record OrderDataView(
        UUID customerId,
        String fullName,
        String email,
        CustomerStatus status,
        @Nullable Address shippingAddress) {}
