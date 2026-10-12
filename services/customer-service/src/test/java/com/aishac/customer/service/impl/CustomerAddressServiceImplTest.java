package com.aishac.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.aishac.customer.dto.AddressRequest;
import com.aishac.customer.dto.AddressResponse;
import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerAddress;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.exception.CustomerErrorCode;
import com.aishac.customer.exception.ErrorCode;
import com.aishac.customer.mapper.CustomerMapper;
import com.aishac.customer.repository.CustomerRepository;
import com.aishac.customer.service.CustomerRules;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Regras de endereço (RN-11 a RN-15), sem Spring nem banco. */
class CustomerAddressServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
    private static final UUID ID = UUID.fromString("0192f0c4-7d1e-7a3b-9c2d-4e5f6a7b8c9d");

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerAddressServiceImpl service =
            new CustomerAddressServiceImpl(repository, Mappers.getMapper(CustomerMapper.class));

    private Customer customer(CustomerStatus status) {
        Customer customer = Customer.builder()
                .fullName("Ana Souza")
                .cpf("52998224725")
                .email("ana@example.com")
                .status(status)
                .version(0L)
                .addresses(new ArrayList<>())
                .build();
        customer.setId(ID);
        given(repository.findForAddressChange(ID)).willReturn(Optional.of(customer));
        given(repository.findWithAddressesById(ID)).willReturn(Optional.of(customer));
        return customer;
    }

    /** Endereço já gravado, criado {@code secondsAfterNow} segundos depois de NOW. */
    private static CustomerAddress existing(Customer customer, String label, boolean isDefault, int secondsAfterNow) {
        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .label(label)
                .zipCode("01310100")
                .street("Rua A")
                .number("1")
                .district("Centro")
                .city("São Paulo")
                .state("SP")
                .defaultAddress(isDefault)
                .build();
        address.setId(UUID.randomUUID());
        address.setCreatedAt(NOW.plusSeconds(secondsAfterNow));
        customer.getAddresses().add(address);
        return address;
    }

    private static AddressRequest request(String label) {
        return new AddressRequest(
                label, "01310-100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "sp");
    }

    private static void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).extracting("errorCode").isEqualTo(code);
    }

    // CA-15
    @Test
    void shouldMakeOnlyTheFirstAddressDefault() {
        Customer customer = customer(CustomerStatus.ACTIVE);

        AddressResponse first = service.add(ID, request("Casa"));
        AddressResponse second = service.add(ID, request("Trabalho"));

        assertThat(first.isDefault()).isTrue();
        assertThat(first.zipCode()).isEqualTo("01310100");
        assertThat(first.state()).isEqualTo("SP");
        assertThat(first.country()).isEqualTo("BR");
        assertThat(second.isDefault()).isFalse();
        assertThat(customer.getAddresses()).hasSize(2);
    }

    // CA-16
    @Test
    void shouldKeepASingleDefaultWhenSwitching() {
        Customer customer = customer(CustomerStatus.ACTIVE);
        CustomerAddress a = existing(customer, "A", true, 0);
        CustomerAddress b = existing(customer, "B", false, 1);

        service.setDefault(ID, b.getId());

        assertThat(b.isDefaultAddress()).isTrue();
        assertThat(a.isDefaultAddress()).isFalse();
        assertThat(service.list(ID).getFirst().id()).isEqualTo(b.getId());
    }

    // CA-17
    @Test
    void shouldPromoteMostRecentWhenDefaultIsRemoved() {
        Customer customer = customer(CustomerStatus.ACTIVE);
        CustomerAddress a = existing(customer, "A", true, 0);
        existing(customer, "B", false, 1);
        CustomerAddress c = existing(customer, "C", false, 2);

        service.remove(ID, a.getId());

        assertThat(customer.getAddresses()).hasSize(2);
        assertThat(c.isDefaultAddress()).isTrue();
    }

    // CA-18
    @Test
    void shouldLimitAddressesToTen() {
        Customer customer = customer(CustomerStatus.ACTIVE);
        for (int i = 0; i < CustomerRules.MAX_ADDRESSES; i++) {
            existing(customer, "E" + i, i == 0, i);
        }

        assertError(() -> service.add(ID, request("Extra")), CustomerErrorCode.ADDRESS_LIMIT_EXCEEDED);
    }

    // CA-14
    @Test
    void shouldRejectAddressChangesWhenCustomerIsInactive() {
        Customer customer = customer(CustomerStatus.INACTIVE);
        CustomerAddress home = existing(customer, "Casa", true, 0);

        assertError(() -> service.add(ID, request("Casa 2")), CustomerErrorCode.CUSTOMER_INACTIVE);
        assertError(() -> service.update(ID, home.getId(), request("Casa 3")), CustomerErrorCode.CUSTOMER_INACTIVE);
        assertError(() -> service.remove(ID, home.getId()), CustomerErrorCode.CUSTOMER_INACTIVE);
        assertError(() -> service.setDefault(ID, home.getId()), CustomerErrorCode.CUSTOMER_INACTIVE);
    }

    // CA-19
    @Test
    void shouldUpdateAddressAndRejectUnknownOne() {
        Customer customer = customer(CustomerStatus.ACTIVE);
        CustomerAddress home = existing(customer, "Casa", true, 0);

        AddressResponse updated = service.update(ID, home.getId(), request("Casa da praia"));

        assertThat(updated.label()).isEqualTo("Casa da praia");
        assertThat(updated.isDefault()).isTrue();
        assertError(() -> service.update(ID, UUID.randomUUID(), request("X")), CustomerErrorCode.ADDRESS_NOT_FOUND);
    }
}
