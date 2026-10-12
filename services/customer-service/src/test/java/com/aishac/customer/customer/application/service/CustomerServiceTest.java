package com.aishac.customer.customer.application.service;

import static com.aishac.customer.customer.CustomerTestBuilder.NOW;
import static com.aishac.customer.customer.CustomerTestBuilder.OTHER_VALID_CPF;
import static com.aishac.customer.customer.CustomerTestBuilder.aCustomer;
import static com.aishac.customer.customer.CustomerTestBuilder.anAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aishac.customer.customer.application.port.in.CustomerCommands.AddAddress;
import com.aishac.customer.customer.application.port.in.CustomerCommands.BlockCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.RegisterCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateAddress;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateCustomer;
import com.aishac.customer.customer.application.port.out.CustomerFilter;
import com.aishac.customer.customer.application.port.out.CustomerPorts.CustomerQueryPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.LoadCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.SaveCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerView;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerErrorCode;
import com.aishac.customer.customer.domain.CustomerException;
import com.aishac.customer.customer.domain.CustomerStatus;
import com.aishac.customer.shared.application.IdGenerator;
import com.aishac.customer.shared.application.PageQuery;
import com.aishac.customer.shared.application.PageResult;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Casos de uso com os ports simulados: orquestração, checagens prévias e erros (sem Spring). */
class CustomerServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("0192f0c4-7d1e-7a3b-9c2d-4e5f6a7b8c9d");
    private static final UUID NEW_ID = UUID.fromString("0192f0c5-1a2b-7c3d-8e4f-5a6b7c8d9e0f");

    private final LoadCustomerPort load = mock(LoadCustomerPort.class);
    private final SaveCustomerPort save = mock(SaveCustomerPort.class);
    private final CustomerQueryPort queryPort = mock(CustomerQueryPort.class);
    private final IdGenerator ids = () -> NEW_ID;
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CustomerService customers = new CustomerService(load, save, ids, clock);
    private final CustomerAddressService addresses = new CustomerAddressService(load, save, ids, clock);
    private final CustomerQueryService queries = new CustomerQueryService(queryPort, load);

    private static void assertError(Runnable action, CustomerErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomerException.class)
                .extracting(e -> ((CustomerException) e).errorCode())
                .isEqualTo(code);
    }

    private Customer stored() {
        Customer customer = aCustomer().withId(CUSTOMER_ID).build();
        given(load.load(CUSTOMER_ID)).willReturn(Optional.of(customer));
        given(save.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        return customer;
    }

    @Test
    void shouldRegisterWithGeneratedIdAndClock() {
        // Given
        given(save.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // When
        Customer customer = customers.register(
                new RegisterCustomer("Ana Souza", "529.982.247-25", "Ana@Example.com", "11987654321"));

        // Then
        assertThat(customer.getId()).isEqualTo(NEW_ID);
        assertThat(customer.getCreatedAt()).isEqualTo(NOW);
        assertThat(customer.getEmail().value()).isEqualTo("ana@example.com");
    }

    // CA-03, CA-04: checagem prévia (a garantia em corrida é a UNIQUE no banco)
    @Test
    void shouldRejectDuplicatedCpfOrEmailBeforeSaving() {
        given(load.existsByCpf(any())).willReturn(true);
        assertError(
                () -> customers.register(new RegisterCustomer("Ana Souza", OTHER_VALID_CPF, "a@x.com", null)),
                CustomerErrorCode.CPF_ALREADY_EXISTS);

        given(load.existsByCpf(any())).willReturn(false);
        given(load.existsByEmail(any())).willReturn(true);
        assertError(
                () -> customers.register(new RegisterCustomer("Ana Souza", OTHER_VALID_CPF, "a@x.com", null)),
                CustomerErrorCode.EMAIL_ALREADY_EXISTS);
        verify(save, never()).save(any());
    }

    // CA-10
    @Test
    void shouldRejectStaleVersionWithoutSaving() {
        // Given
        stored();

        // Then
        assertError(
                () -> customers.update(new UpdateCustomer(CUSTOMER_ID, "Ana Lima", "ana@x.com", null, 3)),
                CustomerErrorCode.CONCURRENT_MODIFICATION);
        verify(save, never()).save(any());
    }

    @Test
    void shouldRejectEmailOfAnotherCustomerOnUpdate() {
        stored();
        given(load.existsByEmailOfAnotherCustomer(any(), any())).willReturn(true);

        assertError(
                () -> customers.update(new UpdateCustomer(CUSTOMER_ID, "Ana Lima", "bia@x.com", null, 0)),
                CustomerErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void shouldChangeStatusThroughTheAggregate() {
        stored();

        assertThat(customers.block(new BlockCustomer(CUSTOMER_ID, "Fraude")).getStatus())
                .isEqualTo(CustomerStatus.BLOCKED);
        assertThat(customers.unblock(CUSTOMER_ID).getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customers.deactivate(CUSTOMER_ID).getStatus()).isEqualTo(CustomerStatus.INACTIVE);
    }

    // CA-06
    @Test
    void shouldAnswerNotFoundForUnknownCustomer() {
        given(load.load(any())).willReturn(Optional.empty());
        given(queryPort.find(any())).willReturn(Optional.empty());

        assertError(() -> customers.unblock(CUSTOMER_ID), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        assertError(() -> addresses.list(CUSTOMER_ID), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        assertError(() -> queries.get(CUSTOMER_ID), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        assertError(() -> queries.orderData(CUSTOMER_ID, null), CustomerErrorCode.CUSTOMER_NOT_FOUND);
    }

    @Test
    void shouldManageAddressesThroughTheAggregate() {
        // Given
        stored();

        // When
        var added = addresses.add(new AddAddress(CUSTOMER_ID, anAddress("Casa")));
        var updated = addresses.update(new UpdateAddress(CUSTOMER_ID, added.getId(), anAddress("Casa nova")));
        addresses.makeDefault(CUSTOMER_ID, added.getId());

        // Then
        assertThat(updated.getData().label()).isEqualTo("Casa nova");
        assertThat(addresses.list(CUSTOMER_ID)).hasSize(1);

        // When
        addresses.remove(CUSTOMER_ID, added.getId());

        // Then
        assertThat(addresses.list(CUSTOMER_ID)).isEmpty();
    }

    // CA-20, CA-21
    @Test
    void shouldComposeOrderDataWithDefaultChosenOrNoAddress() {
        // Given
        Customer customer = stored();

        // Then: sem endereços
        assertThat(queries.orderData(CUSTOMER_ID, null).shippingAddress()).isNull();

        // Given
        customer.addAddress(NEW_ID, anAddress("Casa"), NOW);

        // Then
        assertThat(queries.orderData(CUSTOMER_ID, null).shippingAddress().getId())
                .isEqualTo(NEW_ID);
        assertThat(queries.orderData(CUSTOMER_ID, NEW_ID).status()).isEqualTo(CustomerStatus.ACTIVE);
        assertError(() -> queries.orderData(CUSTOMER_ID, CUSTOMER_ID), CustomerErrorCode.ADDRESS_NOT_FOUND);
    }

    @Test
    void shouldDelegateSearchToQueryPort() {
        PageResult<CustomerView> page = new PageResult<>(List.of(), 0, 20, 0, 0);
        var query = new PageQuery(0, 20, "createdAt", false);
        var filter = new CustomerFilter("  ", null, null, null);
        given(queryPort.search(filter, query)).willReturn(page);

        assertThat(queries.search(filter, query)).isSameAs(page);
        assertThat(filter.name()).isNull();
    }
}
