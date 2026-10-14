package com.aishac.sales.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aishac.sales.AbstractIntegrationTest;
import com.aishac.sales.catalog.TestImages;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * CA-23, RN-22: o adapter real, pelo proxy do Spring, com o S3 fora do ar (porta sem ninguém escutando). Prova que a
 * falha do SDK vira {@code IMAGE_STORAGE_UNAVAILABLE} pelo fallback e que, com o circuito aberto, a chamada nem chega
 * ao S3. Janela reduzida só aqui, para o teste ser rápido.
 */
@TestPropertySource(
        properties = {
            "aishac.storage.endpoint=http://127.0.0.1:1",
            "aishac.storage.public-endpoint=http://127.0.0.1:1",
            "resilience4j.circuitbreaker.instances.s3.sliding-window-size=4",
            "resilience4j.circuitbreaker.instances.s3.minimum-number-of-calls=4",
            "resilience4j.circuitbreaker.instances.s3.wait-duration-in-open-state=1m"
        })
class S3CircuitBreakerIT extends AbstractIntegrationTest {

    static {
        // Credenciais fictícias: sem elas o SDK falharia antes da rede, procurando credenciais (inclusive no IMDS).
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @Autowired
    private S3ProductImageStorage storage;

    @Autowired
    private CircuitBreakerRegistry registry;

    @AfterEach
    void resetCircuit() {
        registry.circuitBreaker("s3").reset();
    }

    /** Os IT dividem a JVM: as credenciais fictícias não podem valer para as classes seguintes. */
    @AfterAll
    static void clearCredentials() {
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
    }

    @Test
    void shouldTranslateFailuresAndOpenTheCircuit() {
        // Given
        CircuitBreaker circuit = registry.circuitBreaker("s3");
        byte[] content = TestImages.jpeg(64);

        // When: falhas reais do SDK, de upload e de remoção
        for (String key : new String[] {"products/x/1.jpg", "products/x/2.jpg"}) {
            assertUnavailable(() -> storage.put(key, content, "image/jpeg"));
            assertUnavailable(() -> storage.delete(key));
        }

        // Then
        assertThat(circuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuit.getMetrics().getNumberOfFailedCalls()).isEqualTo(4);

        // When: circuito aberto
        assertUnavailable(() -> storage.put("products/x/aberto.jpg", content, "image/jpeg"));

        // Then: recusada pelo circuito, sem chamar o S3
        assertThat(circuit.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
        assertThat(circuit.getMetrics().getNumberOfFailedCalls()).isEqualTo(4);
    }

    @Test
    void shouldPresignWithoutCallingStorage() {
        assertThat(storage.presign("products/x/a.jpg").url().toString())
                .startsWith("http://127.0.0.1:1/aishac-product-images-local/products/x/a.jpg?");
        assertThat(registry.circuitBreaker("s3").getMetrics().getNumberOfBufferedCalls())
                .isZero();
    }

    private static void assertUnavailable(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.IMAGE_STORAGE_UNAVAILABLE);
    }
}
