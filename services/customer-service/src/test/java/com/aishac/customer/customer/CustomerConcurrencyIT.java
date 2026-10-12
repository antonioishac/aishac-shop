package com.aishac.customer.customer;

import static com.aishac.customer.customer.CpfFixtures.nextValidCpf;
import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.customer.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Corridas reais com Virtual Threads: as garantias vêm do banco (UNIQUE) e do lock otimista na raiz do agregado,
 * não da checagem prévia no serviço.
 */
class CustomerConcurrencyIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int PARALLEL = 10;

    @LocalServerPort
    private int port;

    // RN-03, CA-03 sob concorrência: só um cadastro vence; os demais recebem CPF_ALREADY_EXISTS
    @Test
    void shouldKeepCpfUniqueUnderConcurrentRegistrations() throws Exception {
        // Given
        String cpf = nextValidCpf();

        // When
        List<ResponseEntity<String>> responses = race(i -> post(
                "/api/v1/customers",
                Map.of(
                        "fullName", "Cliente Concorrente " + i,
                        "cpf", cpf,
                        "email", "race-" + UUID.randomUUID() + "@x.com")));

        // Then
        assertThat(responses).filteredOn(r -> r.getStatusCode().value() == 201).hasSize(1);
        assertThat(responses).filteredOn(r -> r.getStatusCode().value() != 201).allSatisfy(r -> {
            assertThat(r.getStatusCode().value()).isEqualTo(409);
            assertThat(code(r)).isEqualTo("CPF_ALREADY_EXISTS");
        });
    }

    // RN-12 sob concorrência: com 9 endereços, inclusões simultâneas nunca passam de 10
    @Test
    void shouldNeverExceedAddressLimitUnderConcurrentInclusions() throws Exception {
        // Given
        String customer = "/api/v1/customers/"
                + JSON.readTree(post(
                                        "/api/v1/customers",
                                        Map.of(
                                                "fullName",
                                                "Ana Souza",
                                                "cpf",
                                                nextValidCpf(),
                                                "email",
                                                "lim-" + UUID.randomUUID() + "@x.com"))
                                .getBody())
                        .path("id")
                        .asString();
        for (int i = 0; i < 9; i++) {
            assertThat(post(customer + "/addresses", address("E" + i))
                            .getStatusCode()
                            .value())
                    .isEqualTo(201);
        }

        // When
        List<ResponseEntity<String>> responses = race(i -> post(customer + "/addresses", address("R" + i)));

        // Then
        assertThat(responses).filteredOn(r -> r.getStatusCode().value() == 201).hasSize(1);
        assertThat(responses)
                .filteredOn(r -> r.getStatusCode().value() != 201)
                .extracting(CustomerConcurrencyIT::code)
                .containsOnly("CONCURRENT_MODIFICATION", "ADDRESS_LIMIT_EXCEEDED");
        JsonNode stored = JSON.readTree(
                RestClient.create(url(customer + "/addresses")).get().retrieve().body(String.class));
        assertThat(stored).hasSize(10);
    }

    private List<ResponseEntity<String>> race(IntFunction<ResponseEntity<String>> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < PARALLEL; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    return request.apply(index);
                }));
            }
            start.countDown();
            List<ResponseEntity<String>> responses = new ArrayList<>();
            for (Future<ResponseEntity<String>> future : futures) {
                responses.add(future.get());
            }
            return responses;
        }
    }

    private ResponseEntity<String> post(String path, Map<String, Object> body) {
        return RestClient.create(url(path))
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(JSON.writeValueAsString(body))
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);
    }

    private static Map<String, Object> address(String label) {
        return Map.of(
                "label", label,
                "zipCode", "01310100",
                "street", "Rua A",
                "number", "1",
                "district", "Centro",
                "city", "São Paulo",
                "state", "SP");
    }

    private static String code(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody()).path("code").asString();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
