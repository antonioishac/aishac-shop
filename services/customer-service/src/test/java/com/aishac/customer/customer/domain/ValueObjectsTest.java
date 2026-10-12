package com.aishac.customer.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** RN-04 (e-mail), telefone (decisão 4), UF e endereço (RN-11). */
class ValueObjectsTest {

    @Test
    void shouldNormalizeEmailToLowerCaseWithoutSurroundingSpaces() {
        assertThat(Email.of("  Ana@Example.COM ").value()).isEqualTo("ana@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "ana@example", "a na@example.com"})
    void shouldRejectMalformedEmail(String raw) {
        assertThatThrownBy(() -> Email.of(raw))
                .isInstanceOf(CustomerException.class)
                .hasFieldOrPropertyWithValue("field", "email");
    }

    @Test
    void shouldTreatBlankPhoneAsAbsentAndValidateDigits() {
        assertThat(Phone.ofNullable(null)).isNull();
        assertThat(Phone.ofNullable("  ")).isNull();
        assertThat(Phone.ofNullable("1133334444").value()).isEqualTo("1133334444");
        assertThatThrownBy(() -> Phone.ofNullable("119876"))
                .isInstanceOf(CustomerException.class)
                .hasFieldOrPropertyWithValue("field", "phone");
    }

    @Test
    void shouldParseStateCodeIgnoringCase() {
        assertThat(BrazilianState.of(" sp ")).isEqualTo(BrazilianState.SP);
        assertThatThrownBy(() -> BrazilianState.of("XX"))
                .isInstanceOf(CustomerException.class)
                .hasFieldOrPropertyWithValue("field", "state");
    }

    @Test
    void shouldNormalizeAddressData() {
        // When
        var data = new AddressData(" Casa ", "01310-100", "Rua A", "S/N", "  ", "Centro", "Recife", BrazilianState.PE);

        // Then
        assertThat(data.label()).isEqualTo("Casa");
        assertThat(data.zipCode()).isEqualTo("01310100");
        assertThat(data.complement()).isNull();
    }

    @Test
    void shouldRejectInvalidAddressData() {
        assertThatThrownBy(() -> new AddressData("Casa", "1310-100", "Rua", "1", null, "C", "R", BrazilianState.PE))
                .hasFieldOrPropertyWithValue("field", "zipCode");
        assertThatThrownBy(() -> new AddressData(" ", "01310100", "Rua", "1", null, "C", "R", BrazilianState.PE))
                .hasFieldOrPropertyWithValue("field", "label");
        assertThatThrownBy(() ->
                        new AddressData("Casa", "01310100", "Rua", "12345678901", null, "C", "R", BrazilianState.PE))
                .hasFieldOrPropertyWithValue("field", "number");
    }

    @Test
    void shouldAllowOnlyDocumentedStatusTransitions() {
        assertThat(CustomerStatus.ACTIVE.canTransitionTo(CustomerStatus.BLOCKED))
                .isTrue();
        assertThat(CustomerStatus.BLOCKED.canTransitionTo(CustomerStatus.ACTIVE))
                .isTrue();
        assertThat(CustomerStatus.BLOCKED.canTransitionTo(CustomerStatus.INACTIVE))
                .isTrue();
        assertThat(CustomerStatus.ACTIVE.canTransitionTo(CustomerStatus.ACTIVE)).isFalse();
        assertThat(CustomerStatus.INACTIVE.canTransitionTo(CustomerStatus.ACTIVE))
                .isFalse();
    }
}
