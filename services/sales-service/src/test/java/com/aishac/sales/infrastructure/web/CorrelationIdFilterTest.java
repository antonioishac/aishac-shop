package com.aishac.sales.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldKeepIncomingCorrelationIdInMdcDuringRequestAndClearItAfter() throws Exception {
        // Given
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
        var response = new MockHttpServletResponse();
        var seenInChain = new AtomicReference<String>();

        // When
        filter.doFilter(request, response, (req, res) -> seenInChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        // Then
        assertThat(seenInChain.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsTooLong() throws Exception {
        // Given
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "a".repeat(65));
        var response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, (req, res) -> {});

        // Then
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }
}
