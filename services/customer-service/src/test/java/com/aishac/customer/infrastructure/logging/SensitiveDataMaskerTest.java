package com.aishac.customer.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SensitiveDataMaskerTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "cpf 123.456.789-09 informado | cpf ***.***.***-09 informado",
                "cpf 12345678909 informado    | cpf ***.***.***-09 informado",
                "e-mail joao.silva@example.com | e-mail j***@example.com",
                "123.456.789-09 e ana@shop.com.br | ***.***.***-09 e a***@shop.com.br"
            })
    void shouldMaskCpfAndEmail(String text, String expected) {
        // When
        String masked = SensitiveDataMasker.mask(text);

        // Then
        assertThat(masked).isEqualTo(expected);
    }

    @Test
    void shouldKeepIdentifiersThatAreNotCpf() {
        // Given
        String text = "order 123456789012345 correlation 0192a3b4-5c6d-7e8f-9a0b-1c2d3e4f5a6b";

        // When
        String masked = SensitiveDataMasker.mask(text);

        // Then
        assertThat(masked).isEqualTo(text);
    }
}
