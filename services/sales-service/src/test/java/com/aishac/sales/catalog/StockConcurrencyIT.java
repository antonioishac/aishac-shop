package com.aishac.sales.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.sales.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Saídas de estoque simultâneas contra PostgreSQL real (CA-14). */
class StockConcurrencyIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int REQUESTS = 10;

    @LocalServerPort
    private int port;

    // CA-14
    @Test
    void shouldAcceptExactlyOneExitWhenLastItemIsDisputed() throws Exception {
        // Given
        String productId = productWithStock(1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();

        // When
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return post(
                            "/api/v1/products/" + productId + "/stock-movements",
                            Map.of("type", "EXIT", "quantity", 1, "reason", "Venda simultânea"));
                }));
            }
            start.countDown();
        }

        // Then
        List<Integer> statuses = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (Future<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get();
            statuses.add(response.getStatusCode().value());
            if (response.getStatusCode().value() != 201) {
                codes.add(json(response).path("code").asString());
            }
        }
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(codes).hasSize(REQUESTS - 1).containsOnly("INSUFFICIENT_STOCK");
        JsonNode product = json(
                client().get().uri("/api/v1/products/" + productId).retrieve().toEntity(String.class));
        assertThat(product.path("stockQuantity").asInt()).isZero();
    }

    private String productWithStock(int stock) {
        String categoryId = json(post(
                        "/api/v1/categories",
                        Map.of(
                                "name",
                                "Concorrência " + UUID.randomUUID().toString().substring(0, 8))))
                .path("id")
                .asString();
        String sku = "C-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return json(post(
                        "/api/v1/products",
                        Map.of(
                                "sku", sku,
                                "name", "Último item",
                                "price", "10.00",
                                "categoryId", categoryId,
                                "initialStock", stock)))
                .path("id")
                .asString();
    }

    private ResponseEntity<String> post(String path, Object body) {
        return client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {})
                .toEntity(String.class);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private static JsonNode json(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody());
    }
}
