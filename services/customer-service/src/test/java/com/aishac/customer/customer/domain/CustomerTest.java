package com.aishac.customer.customer.domain;

import static com.aishac.customer.customer.CustomerTestBuilder.NOW;
import static com.aishac.customer.customer.CustomerTestBuilder.aCustomer;
import static com.aishac.customer.customer.CustomerTestBuilder.anAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final Instant LATER = NOW.plusSeconds(60);

    private static UUID id(int n) {
        return UUID.fromString("00000000-0000-7000-8000-%012d".formatted(n));
    }

    private static void assertError(Runnable action, CustomerErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomerException.class)
                .extracting(e -> ((CustomerException) e).errorCode())
                .isEqualTo(expected);
    }

    // CA-01, RN-05
    @Test
    void shouldRegisterActiveCustomerWithNormalizedData() {
        // When
        Customer customer = aCustomer()
                .withFullName("  Ana Souza ")
                .withEmail("Ana@Example.com ")
                .build();

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getFullName()).isEqualTo("Ana Souza");
        assertThat(customer.getEmail().value()).isEqualTo("ana@example.com");
        assertThat(customer.getVersion()).isZero();
        assertThat(customer.getAddresses()).isEmpty();
        assertThat(customer.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectTooShortName() {
        assertThatThrownBy(() -> aCustomer().withFullName(" Al ").build())
                .isInstanceOf(CustomerException.class)
                .hasFieldOrPropertyWithValue("field", "fullName");
    }

    // CA-09
    @Test
    void shouldUpdateDetailsKeepingCpf() {
        // Given
        Customer customer = aCustomer().build();
        Cpf originalCpf = customer.getCpf();

        // When
        customer.updateDetails("Ana Souza Lima", Email.of("ana.lima@example.com"), null, LATER);

        // Then
        assertThat(customer.getFullName()).isEqualTo("Ana Souza Lima");
        assertThat(customer.getEmail().value()).isEqualTo("ana.lima@example.com");
        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getCpf()).isEqualTo(originalCpf);
        assertThat(customer.getUpdatedAt()).isEqualTo(LATER);
    }

    // CA-11
    @Test
    void shouldBlockWithReasonAndUnblock() {
        // Given
        Customer customer = aCustomer().build();

        // When
        customer.block("  Suspeita de fraude ", LATER);

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
        assertThat(customer.getBlockReason()).isEqualTo("Suspeita de fraude");
        assertThat(customer.getBlockedAt()).isEqualTo(LATER);

        // When
        customer.unblock(LATER.plusSeconds(1));

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getBlockReason()).isNull();
        assertThat(customer.getBlockedAt()).isNull();
    }

    @Test
    void shouldRequireBlockReason() {
        Customer customer = aCustomer().build();
        assertThatThrownBy(() -> customer.block(" a ", LATER)).hasFieldOrPropertyWithValue("field", "reason");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    // CA-12
    @Test
    void shouldDeactivateBlockedCustomerClearingBlockData() {
        // Given
        Customer customer = aCustomer().build();
        customer.block("Suspeita de fraude", NOW);

        // When
        customer.deactivate(LATER);

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(customer.getDeactivatedAt()).isEqualTo(LATER);
        assertThat(customer.getBlockReason()).isNull();
    }

    // CA-13
    @Test
    void shouldRejectInvalidTransitions() {
        Customer active = aCustomer().build();
        assertError(() -> active.unblock(LATER), CustomerErrorCode.INVALID_STATUS_TRANSITION);

        Customer inactive = aCustomer().build();
        inactive.deactivate(NOW);
        assertError(() -> inactive.block("Motivo", LATER), CustomerErrorCode.INVALID_STATUS_TRANSITION);
        assertError(() -> inactive.unblock(LATER), CustomerErrorCode.INVALID_STATUS_TRANSITION);
        assertError(() -> inactive.deactivate(LATER), CustomerErrorCode.INVALID_STATUS_TRANSITION);
    }

    // CA-14
    @Test
    void shouldRejectChangesWhenInactive() {
        // Given
        Customer customer = aCustomer().build();
        customer.addAddress(id(1), anAddress("Casa"), NOW);
        customer.deactivate(NOW);

        // Then
        Consumer<Runnable> rejected = action -> assertError(action, CustomerErrorCode.CUSTOMER_INACTIVE);
        rejected.accept(() -> customer.updateDetails("Outro Nome", Email.of("x@example.com"), null, LATER));
        rejected.accept(() -> customer.addAddress(id(2), anAddress("Trabalho"), LATER));
        rejected.accept(() -> customer.updateAddress(id(1), anAddress("Casa nova"), LATER));
        rejected.accept(() -> customer.removeAddress(id(1), LATER));
        rejected.accept(() -> customer.makeDefault(id(1), LATER));
    }

    // CA-15
    @Test
    void shouldMakeFirstAddressDefault() {
        // Given
        Customer customer = aCustomer().build();

        // When
        Address first = customer.addAddress(id(1), anAddress("Casa"), LATER);
        Address second = customer.addAddress(id(2), anAddress("Trabalho"), LATER);

        // Then
        assertThat(first.isDefault()).isTrue();
        assertThat(second.isDefault()).isFalse();
        assertThat(customer.getUpdatedAt()).isEqualTo(LATER);
    }

    // CA-16
    @Test
    void shouldSwitchDefaultAddressKeepingOnlyOne() {
        // Given
        Customer customer = aCustomer().build();
        customer.addAddress(id(1), anAddress("A"), NOW);
        customer.addAddress(id(2), anAddress("B"), NOW);

        // When
        customer.makeDefault(id(2), LATER);

        // Then
        assertThat(customer.defaultAddress()).map(Address::getId).contains(id(2));
        assertThat(customer.getAddresses()).filteredOn(Address::isDefault).hasSize(1);
        assertThat(customer.getAddresses().getFirst().getId()).isEqualTo(id(2));
    }

    // CA-17
    @Test
    void shouldPromoteMostRecentWhenDefaultIsRemoved() {
        // Given
        Customer customer = aCustomer().build();
        customer.addAddress(id(1), anAddress("A"), NOW);
        customer.addAddress(id(2), anAddress("B"), NOW.plusSeconds(1));
        customer.addAddress(id(3), anAddress("C"), NOW.plusSeconds(2));

        // When
        customer.removeAddress(id(1), LATER);

        // Then
        assertThat(customer.getAddresses()).hasSize(2);
        assertThat(customer.defaultAddress()).map(Address::getId).contains(id(3));
    }

    @Test
    void shouldKeepDefaultWhenRemovingAnotherAddressOrTheLastOne() {
        Customer customer = aCustomer().build();
        customer.addAddress(id(1), anAddress("A"), NOW);
        customer.addAddress(id(2), anAddress("B"), NOW);

        customer.removeAddress(id(2), LATER);
        assertThat(customer.defaultAddress()).map(Address::getId).contains(id(1));

        customer.removeAddress(id(1), LATER);
        assertThat(customer.defaultAddress()).isEmpty();
    }

    // CA-18
    @Test
    void shouldLimitAddressesToTen() {
        // Given
        Customer customer = aCustomer().build();
        for (int i = 1; i <= Customer.MAX_ADDRESSES; i++) {
            customer.addAddress(id(i), anAddress("End " + i), NOW);
        }

        // Then
        assertError(
                () -> customer.addAddress(id(11), anAddress("Extra"), LATER), CustomerErrorCode.ADDRESS_LIMIT_EXCEEDED);
    }

    @Test
    void shouldUpdateAddressAndRejectUnknownOne() {
        // Given
        Customer customer = aCustomer().build();
        customer.addAddress(id(1), anAddress("Casa"), NOW);

        // When
        Address updated = customer.updateAddress(id(1), anAddress("Casa da praia"), LATER);

        // Then
        assertThat(updated.getData().label()).isEqualTo("Casa da praia");
        assertThat(updated.getUpdatedAt()).isEqualTo(LATER);
        assertError(() -> customer.updateAddress(id(9), anAddress("X"), LATER), CustomerErrorCode.ADDRESS_NOT_FOUND);
    }
}
