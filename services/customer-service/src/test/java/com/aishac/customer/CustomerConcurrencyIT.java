package com.aishac.customer;

import static com.aishac.customer.CpfFixtures.nextValidCpf;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Corridas reais com Virtual Threads. As garantias vêm do banco (UNIQUE) e da trava no cliente durante alterações
 * de endereço, não da checagem prévia no service.
 */
class CustomerConcurrencyIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int PARALLEL = 10;

    @LocalServerPort
    private int port;

    // RN-03 sob concorrência: um cadastro vence; os outros recebem CPF_ALREADY_EXISTS
    @Test
    void shouldKeepCpfUniqueUnderConcurrentRegistrations() throws Exception {
        String cpf = nextValidCpf();

        List<ResponseEntity<String>> responses = race(i -> post(
                "/api/v1/customers",
                Map.of(
                        "fullName",
                        "Cliente Concorrente " + i,
                        "cpf",
                        cpf,
                        "email",
                        "race-" + UUID.randomUUID() + "@x.com")));

        assertSingleWinner(responses, "CPF_ALREADY_EXISTS");
    }

    // RN-04 sob concorrência: um cadastro vence; os outros recebem EMAIL_ALREADY_EXISTS
    @Test
    void shouldKeepEmailUniqueUnderConcurrentRegistrations() throws Exception {
        String email = "race-" + UUID.randomUUID() + "@x.com";

        List<ResponseEntity<String>> responses = race(i -> post(
                "/api/v1/customers",
                Map.of("fullName", "Cliente Concorrente " + i, "cpf", nextValidCpf(), "email", email)));

        assertSingleWinner(responses, "EMAIL_ALREADY_EXISTS");
    }

    // RN-12 sob concorrência: com 9 endereços, inclusões simultâneas param exatamente em 10
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

        // Then: a trava no cliente torna as inclusões sequenciais; quem chega depois vê 10 endereços
        assertSingleWinner(responses, "ADDRESS_LIMIT_EXCEEDED");
        String stored =
                RestClient.create(url(customer + "/addresses")).get().retrieve().body(String.class);
        assertThat(JSON.readTree(stored)).hasSize(10);
    }

    // RN-13/RN-14 sob concorrência: trocar o padrão e remover o padrão ao mesmo tempo nunca deixa dois padrões
    @Test
    void shouldKeepExactlyOneDefaultUnderConcurrentChanges() throws Exception {
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
                                                "def-" + UUID.randomUUID() + "@x.com"))
                                .getBody())
                        .path("id")
                        .asString();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(JSON.readTree(
                            post(customer + "/addresses", address("E" + i)).getBody())
                    .path("id")
                    .asString());
        }

        // When: remove o padrão (E0) enquanto outras requisições trocam o padrão entre E1 e E2
        List<ResponseEntity<String>> responses = race(i -> i == 0
                ? exchange("DELETE", customer + "/addresses/" + ids.get(0))
                : exchange("PUT", customer + "/addresses/" + ids.get(1 + i % 2) + "/default"));

        // Then: nenhuma requisição falha por erro interno; só a trava pode recusar com conflito
        assertThat(responses.getFirst().getStatusCode().value()).isEqualTo(204);
        assertThat(responses)
                .allSatisfy(r -> assertThat(r.getStatusCode().value() == 204
                                || JSON.readTree(r.getBody())
                                        .path("code")
                                        .asString()
                                        .equals("CONCURRENT_MODIFICATION"))
                        .as(r.getStatusCode() + " " + r.getBody())
                        .isTrue());
        String stored =
                RestClient.create(url(customer + "/addresses")).get().retrieve().body(String.class);
        long defaults = 0;
        for (var address : JSON.readTree(stored)) {
            if (address.path("isDefault").asBoolean()) {
                defaults++;
            }
        }
        assertThat(defaults).isEqualTo(1);
    }

    private ResponseEntity<String> exchange(String method, String path) {
        return RestClient.create(url(path))
                .method(HttpMethod.valueOf(method))
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);
    }

    private static void assertSingleWinner(List<ResponseEntity<String>> responses, String loserCode) {
        assertThat(responses).filteredOn(r -> r.getStatusCode().value() == 201).hasSize(1);
        assertThat(responses)
                .filteredOn(r -> r.getStatusCode().value() != 201)
                .allSatisfy(
                        r -> assertThat(JSON.readTree(r.getBody()).path("code").asString())
                                .as(r.getBody())
                                .isEqualTo(loserCode));
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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
