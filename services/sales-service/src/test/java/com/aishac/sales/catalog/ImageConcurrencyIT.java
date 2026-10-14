package com.aishac.sales.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.sales.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Alterações simultâneas de imagens do mesmo produto contra PostgreSQL real: a trava do produto
 * ({@code SELECT ... FOR UPDATE}) mantém o limite de 8, posições únicas e uma só principal.
 */
class ImageConcurrencyIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private InMemoryProductImageStorage storage;

    // RN-16, RN-21: só uma vaga sobra; os demais uploads são recusados e não deixam objeto órfão
    @Test
    void shouldAcceptOnlyTheLastFreeSlotUnderConcurrentUploads() throws Exception {
        // Given
        String productId = newProduct();
        for (int i = 1; i <= 7; i++) {
            assertThat(upload(productId).getStatusCode().value()).isEqualTo(201);
        }

        // When
        List<ResponseEntity<String>> responses = concurrently(5, () -> upload(productId));

        // Then
        assertThat(responses).filteredOn(r -> r.getStatusCode().value() == 201).hasSize(1);
        assertThat(responses)
                .filteredOn(r -> r.getStatusCode().value() != 201)
                .extracting(r -> json(r).path("code").asString())
                .containsOnly("IMAGE_LIMIT_EXCEEDED");
        JsonNode images = json(get("/api/v1/products/" + productId + "/images"));
        assertThat(images.size()).isEqualTo(8);
        List<Integer> positions = new ArrayList<>();
        images.forEach(image -> positions.add(image.path("position").asInt()));
        assertThat(positions).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(storage.count("products/" + productId + "/")).isEqualTo(8);
    }

    // RN-17: duas imagens marcadas como principal ao mesmo tempo terminam com exatamente uma principal
    @Test
    void shouldKeepSinglePrimaryUnderConcurrentChanges() throws Exception {
        // Given
        String productId = newProduct();
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            ids.add(json(upload(productId)).path("id").asString());
        }

        // When
        List<Callable<ResponseEntity<String>>> calls = new ArrayList<>();
        for (String id : ids.subList(1, 4)) {
            calls.add(() -> exchange(
                    HttpMethod.PATCH, "/api/v1/products/" + productId + "/images/" + id, Map.of("primary", true)));
        }
        List<ResponseEntity<String>> responses = concurrently(calls);

        // Then
        assertThat(responses)
                .allSatisfy(r -> assertThat(r.getStatusCode().value()).isEqualTo(200));
        JsonNode images = json(get("/api/v1/products/" + productId + "/images"));
        List<String> primaries = new ArrayList<>();
        images.forEach(image -> {
            if (image.path("primary").asBoolean()) {
                primaries.add(image.path("id").asString());
            }
        });
        assertThat(primaries).hasSize(1).isSubsetOf(ids.subList(1, 4));
    }

    private List<ResponseEntity<String>> concurrently(int count, Callable<ResponseEntity<String>> call)
            throws Exception {
        List<Callable<ResponseEntity<String>>> calls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            calls.add(call);
        }
        return concurrently(calls);
    }

    private static List<ResponseEntity<String>> concurrently(List<Callable<ResponseEntity<String>>> calls)
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Callable<ResponseEntity<String>> call : calls) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();
        }
        List<ResponseEntity<String>> responses = new ArrayList<>();
        for (Future<ResponseEntity<String>> future : futures) {
            responses.add(future.get());
        }
        return responses;
    }

    private String newProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String categoryId = json(exchange(HttpMethod.POST, "/api/v1/categories", Map.of("name", "Imagens " + suffix)))
                .path("id")
                .asString();
        return json(exchange(
                        HttpMethod.POST,
                        "/api/v1/products",
                        Map.of(
                                "sku",
                                "IMG-" + suffix.toUpperCase(),
                                "name",
                                "Produto com imagens",
                                "price",
                                "10.00",
                                "categoryId",
                                categoryId,
                                "initialStock",
                                0)))
                .path("id")
                .asString();
    }

    private ResponseEntity<String> upload(String productId) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new ByteArrayResource(TestImages.jpeg(64)) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        });
        parts.add("altText", "Foto");
        return client().post()
                .uri("/api/v1/products/" + productId + "/images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {})
                .toEntity(String.class);
    }

    private ResponseEntity<String> get(String path) {
        return exchange(HttpMethod.GET, path, null);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Object body) {
        var request = client().method(method).uri(path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.retrieve()
                .onStatus(status -> true, (req, response) -> {})
                .toEntity(String.class);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private static JsonNode json(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody());
    }
}
