package com.aishac.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CustomerErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CA-02, RN-02. */
class CpfValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", " 529.982.247-25 ", "390.533.447-05"})
    void shouldAcceptValidCpfWithOrWithoutPunctuationKeepingOnlyDigits(String raw) {
        assertThat(CpfValidator.normalize(raw)).matches("\\d{11}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "111.111.111-11", "00000000000", "5299822472", "529.982.247-2a", ""})
    void shouldRejectInvalidCpf(String raw) {
        assertThatThrownBy(() -> CpfValidator.normalize(raw))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CustomerErrorCode.INVALID_CPF);
    }
}
