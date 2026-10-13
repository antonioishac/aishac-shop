package com.aishac.customer.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

/** A resposta de um CHECK violado aponta o campo da API, nunca o nome da constraint do banco. */
class ConstraintViolationsTest {

    @ParameterizedTest
    @CsvSource({
        "ck_customers_full_name_length, fullName",
        "ck_customers_cpf_format, cpf",
        "ck_customers_email_normalized, email",
        "ck_customers_phone_format, phone",
        "ck_customer_addresses_label_not_blank, label",
        "ck_customer_addresses_zip_code_format, zipCode",
        "ck_customer_addresses_state, state",
        "ck_customer_addresses_country, country",
        "ck_customers_block_data, request",
        "ck_customers_status, request"
    })
    void shouldMapEachCheckConstraintToTheApiField(String constraint, String field) {
        assertThat(ConstraintViolations.apiField(constraint)).isEqualTo(field);
    }

    @ParameterizedTest
    @CsvSource({"ck_customers_cpf_format, true", "uk_customers_cpf, false"})
    void shouldReadTheConstraintNameFromHibernate(String constraint, boolean check) {
        // Given
        var exception = new DataIntegrityViolationException(
                "violation", new ConstraintViolationException("violation", new SQLException(), constraint));

        // When
        String name = ConstraintViolations.constraintName(exception).orElseThrow();

        // Then
        assertThat(name).isEqualTo(constraint);
        assertThat(ConstraintViolations.isCheckConstraint(name)).isEqualTo(check);
    }

    @Test
    void shouldAnswerEmptyWhenThereIsNoConstraintName() {
        assertThat(ConstraintViolations.constraintName(new DataIntegrityViolationException("no hibernate cause")))
                .isEmpty();
    }
}
