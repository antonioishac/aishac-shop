package com.aishac.customer.customer.application.port.in;

import com.aishac.customer.customer.domain.AddressData;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Comandos de escrita do contexto Clientes. Valores ainda brutos: os Value Objects validam no serviço. */
public final class CustomerCommands {

    private CustomerCommands() {}

    public record RegisterCustomer(
            String fullName,
            String cpf,
            String email,
            @Nullable String phone) {}

    public record UpdateCustomer(
            UUID customerId,
            String fullName,
            String email,
            @Nullable String phone,
            long expectedVersion) {}

    public record BlockCustomer(UUID customerId, String reason) {}

    public record AddAddress(UUID customerId, AddressData data) {}

    public record UpdateAddress(UUID customerId, UUID addressId, AddressData data) {}
}
