package com.aishac.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aishac.customer.dto.BlockCustomerRequest;
import com.aishac.customer.dto.CustomerRequest;
import com.aishac.customer.dto.CustomerResponse;
import com.aishac.customer.dto.CustomerUpdateRequest;
import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerAddress;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.exception.CommonErrorCode;
import com.aishac.customer.exception.CustomerErrorCode;
import com.aishac.customer.exception.ErrorCode;
import com.aishac.customer.exception.NotFoundException;
import com.aishac.customer.mapper.CustomerMapper;
import com.aishac.customer.repository.CustomerRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;

/** Regras do cadastro de cliente, sem Spring nem banco. */
class CustomerServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
    private static final UUID ID = UUID.fromString("0192f0c4-7d1e-7a3b-9c2d-4e5f6a7b8c9d");

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerServiceImpl service = new CustomerServiceImpl(
            repository, Mappers.getMapper(CustomerMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));

    private static Customer customer(CustomerStatus status) {
        Customer customer = Customer.builder()
                .fullName("Ana Souza")
                .cpf("52998224725")
                .email("ana@example.com")
                .status(status)
                .version(0L)
                .addresses(new ArrayList<>())
                .build();
        customer.setId(ID);
        return customer;
    }

    private Customer stored(CustomerStatus status) {
        Customer customer = customer(status);
        given(repository.findById(ID)).willReturn(Optional.of(customer));
        given(repository.findWithAddressesById(ID)).willReturn(Optional.of(customer));
        given(repository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));
        return customer;
    }

    private static void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).extracting("errorCode").isEqualTo(code);
    }

    // CA-01, RN-02, RN-04, RN-05
    @Test
    void shouldCreateActiveCustomerWithNormalizedCpfAndEmail() {
        // Given
        given(repository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        // When
        CustomerResponse response =
                service.create(new CustomerRequest("Ana Souza", "529.982.247-25", "Ana@Example.COM", "11987654321"));

        // Then
        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(response.cpf()).isEqualTo("52998224725");
        assertThat(response.email()).isEqualTo("ana@example.com");
    }

    // CA-02
    @Test
    void shouldRejectInvalidCpfBeforeTouchingTheDatabase() {
        assertError(
                () -> service.create(new CustomerRequest("Ana Souza", "529.982.247-24", "a@x.com", null)),
                CustomerErrorCode.INVALID_CPF);
        verify(repository, never()).saveAndFlush(any());
    }

    // CA-03, CA-04
    @Test
    void shouldRejectDuplicatedCpfOrEmail() {
        given(repository.existsByCpf("52998224725")).willReturn(true);
        assertError(
                () -> service.create(new CustomerRequest("Ana Souza", "52998224725", "a@x.com", null)),
                CustomerErrorCode.CPF_ALREADY_EXISTS);

        given(repository.existsByCpf(any())).willReturn(false);
        given(repository.existsByEmail("a@x.com")).willReturn(true);
        assertError(
                () -> service.create(new CustomerRequest("Ana Souza", "52998224725", "A@X.com", null)),
                CustomerErrorCode.EMAIL_ALREADY_EXISTS);
    }

    // Corrida: a UNIQUE do banco é traduzida para o código do catálogo
    @Test
    void shouldTranslateUniqueViolationRaisedByConcurrentRegistration() {
        given(repository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException(
                        "duplicate", new SQLException("violates unique constraint \"uk_customers_cpf\"")));

        assertError(
                () -> service.create(new CustomerRequest("Ana Souza", "52998224725", "a@x.com", null)),
                CustomerErrorCode.CPF_ALREADY_EXISTS);
    }

    // CA-09
    @Test
    void shouldUpdateDetailsKeepingCpf() {
        stored(CustomerStatus.ACTIVE);

        CustomerResponse response =
                service.update(ID, new CustomerUpdateRequest("Ana Souza Lima", "ANA.LIMA@x.com", null, 0L));

        assertThat(response.fullName()).isEqualTo("Ana Souza Lima");
        assertThat(response.email()).isEqualTo("ana.lima@x.com");
        assertThat(response.cpf()).isEqualTo("52998224725");
    }

    // CA-10, CA-14, CA-04 na atualização
    @Test
    void shouldRejectStaleVersionInactiveCustomerAndEmailOfAnotherCustomer() {
        stored(CustomerStatus.ACTIVE);
        assertError(
                () -> service.update(ID, new CustomerUpdateRequest("Ana", "a@x.com", null, 3L)),
                CommonErrorCode.CONCURRENT_MODIFICATION);

        given(repository.existsByEmailAndIdNot("bia@x.com", ID)).willReturn(true);
        assertError(
                () -> service.update(ID, new CustomerUpdateRequest("Ana", "bia@x.com", null, 0L)),
                CustomerErrorCode.EMAIL_ALREADY_EXISTS);

        stored(CustomerStatus.INACTIVE);
        assertError(
                () -> service.update(ID, new CustomerUpdateRequest("Ana", "a@x.com", null, 0L)),
                CustomerErrorCode.CUSTOMER_INACTIVE);
    }

    // CA-11, CA-12
    @Test
    void shouldBlockUnblockAndDeactivateWithTimestamps() {
        stored(CustomerStatus.ACTIVE);

        CustomerResponse blocked = service.block(ID, new BlockCustomerRequest("Suspeita de fraude"));
        assertThat(blocked.status()).isEqualTo(CustomerStatus.BLOCKED);
        assertThat(blocked.blockReason()).isEqualTo("Suspeita de fraude");
        assertThat(blocked.blockedAt()).isEqualTo(NOW);

        CustomerResponse deactivated = service.deactivate(ID);
        assertThat(deactivated.status()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(deactivated.deactivatedAt()).isEqualTo(NOW);
        assertThat(deactivated.blockReason()).isNull();
    }

    // CA-13
    @Test
    void shouldRejectInvalidStatusTransitions() {
        stored(CustomerStatus.ACTIVE);
        assertError(() -> service.unblock(ID), CustomerErrorCode.INVALID_STATUS_TRANSITION);

        stored(CustomerStatus.INACTIVE);
        assertError(
                () -> service.block(ID, new BlockCustomerRequest("Motivo")),
                CustomerErrorCode.INVALID_STATUS_TRANSITION);
        assertError(() -> service.deactivate(ID), CustomerErrorCode.INVALID_STATUS_TRANSITION);
    }

    // CA-06
    @Test
    void shouldAnswerNotFoundForUnknownCustomer() {
        given(repository.findById(any())).willReturn(Optional.empty());
        given(repository.findWithAddressesById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID)).isInstanceOf(NotFoundException.class);
        assertError(() -> service.findOrderData(ID, null), CustomerErrorCode.CUSTOMER_NOT_FOUND);
    }

    // CA-20, CA-21
    @Test
    void shouldReturnOrderDataWithDefaultChosenOrNoAddress() {
        // Given
        Customer customer = stored(CustomerStatus.BLOCKED);

        // Then: sem endereços, endereço nulo e status real
        var withoutAddress = service.findOrderData(ID, null);
        assertThat(withoutAddress.shippingAddress()).isNull();
        assertThat(withoutAddress.status()).isEqualTo(CustomerStatus.BLOCKED);

        // Given
        CustomerAddress home = address("Casa", true);
        CustomerAddress work = address("Trabalho", false);
        customer.getAddresses().addAll(List.of(home, work));

        // Then
        assertThat(service.findOrderData(ID, null).shippingAddress().label()).isEqualTo("Casa");
        assertThat(service.findOrderData(ID, work.getId()).shippingAddress().label())
                .isEqualTo("Trabalho");
        assertError(() -> service.findOrderData(ID, UUID.randomUUID()), CustomerErrorCode.ADDRESS_NOT_FOUND);
    }

    private static CustomerAddress address(String label, boolean isDefault) {
        CustomerAddress address = CustomerAddress.builder()
                .label(label)
                .zipCode("01310100")
                .street("Avenida Paulista")
                .number("1000")
                .district("Bela Vista")
                .city("São Paulo")
                .state("SP")
                .defaultAddress(isDefault)
                .build();
        address.setId(UUID.randomUUID());
        return address;
    }
}
