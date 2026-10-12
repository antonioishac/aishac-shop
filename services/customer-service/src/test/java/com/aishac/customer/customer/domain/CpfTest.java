package com.aishac.customer.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CA-02 e RN-02. */
class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", " 529.982.247-25 ", "390.533.447-05"})
    void shouldAcceptValidCpfWithOrWithoutPunctuationKeepingOnlyDigits(String raw) {
        // When
        Cpf cpf = Cpf.of(raw);

        // Then
        assertThat(cpf.value()).matches("\\d{11}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "111.111.111-11", "00000000000", "5299822472", "529.982.247-2a", "abc"})
    void shouldRejectInvalidCpf(String raw) {
        assertThatThrownBy(() -> Cpf.of(raw))
                .isInstanceOf(CustomerException.class)
                .extracting(e -> ((CustomerException) e).errorCode())
                .isEqualTo(CustomerErrorCode.INVALID_CPF);
    }
}
