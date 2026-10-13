package com.aishac.customer.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.Pattern;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** A validação de UF e o enum {@code BrazilianState} publicado no OpenAPI não podem divergir. */
class AddressRequestTest {

    @Test
    void shouldValidateExactlyTheStatesPublishedInTheOpenApi() throws Exception {
        // Given
        Pattern pattern = AddressRequest.class.getDeclaredField("state").getAnnotation(Pattern.class);

        // When
        var accepted = Arrays.asList(pattern.regexp().split("\\|"));

        // Then
        assertThat(accepted).containsExactlyInAnyOrderElementsOf(OpenApiSchemas.BRAZILIAN_STATES);
    }
}
