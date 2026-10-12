package com.aishac.customer.customer;

import com.aishac.customer.customer.domain.AddressData;
import com.aishac.customer.customer.domain.BrazilianState;
import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.customer.domain.Phone;
import java.time.Instant;
import java.util.UUID;

/** Test data builder: {@code aCustomer().withEmail("x@y.com").build()}. */
public final class CustomerTestBuilder {

    public static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
    public static final String VALID_CPF = "52998224725";
    public static final String OTHER_VALID_CPF = "39053344705";

    private UUID id = UUID.fromString("0192f0c4-7d1e-7a3b-9c2d-4e5f6a7b8c9d");
    private String fullName = "Ana Souza";
    private String cpf = VALID_CPF;
    private String email = "ana@example.com";
    private String phone = "11987654321";
    private Instant now = NOW;

    private CustomerTestBuilder() {}

    public static CustomerTestBuilder aCustomer() {
        return new CustomerTestBuilder();
    }

    public static AddressData anAddress(String label) {
        return new AddressData(
                label,
                "01310-100",
                "Avenida Paulista",
                "1000",
                "Apto 42",
                "Bela Vista",
                "São Paulo",
                BrazilianState.SP);
    }

    public CustomerTestBuilder withId(UUID value) {
        this.id = value;
        return this;
    }

    public CustomerTestBuilder withFullName(String value) {
        this.fullName = value;
        return this;
    }

    public CustomerTestBuilder withCpf(String value) {
        this.cpf = value;
        return this;
    }

    public CustomerTestBuilder withEmail(String value) {
        this.email = value;
        return this;
    }

    public CustomerTestBuilder at(Instant value) {
        this.now = value;
        return this;
    }

    public Customer build() {
        return Customer.register(id, fullName, Cpf.of(cpf), Email.of(email), Phone.ofNullable(phone), now);
    }
}
