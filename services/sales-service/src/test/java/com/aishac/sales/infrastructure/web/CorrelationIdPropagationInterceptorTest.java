package com.aishac.sales.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CorrelationIdPropagationInterceptorTest {

    private final RestClient.Builder builder =
            RestClient.builder().requestInterceptor(new CorrelationIdPropagationInterceptor());
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldForwardCurrentCorrelationIdToOutgoingRequest() {
        // Given
        MDC.put(CorrelationIdFilter.MDC_KEY, "abc-123");
        server.expect(header(CorrelationIdFilter.HEADER, "abc-123")).andRespond(withSuccess());

        // When
        builder.build()
                .get()
                .uri("http://customer-service/api/v1/customers")
                .retrieve()
                .toBodilessEntity();

        // Then
        server.verify();
    }

    @Test
    void shouldNotAddHeaderOutsideOfARequest() {
        // Given
        server.expect(headerDoesNotExist(CorrelationIdFilter.HEADER)).andRespond(withSuccess());

        // When
        builder.build()
                .get()
                .uri("http://customer-service/api/v1/customers")
                .retrieve()
                .toBodilessEntity();

        // Then
        server.verify();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
