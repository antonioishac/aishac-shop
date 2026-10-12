package com.aishac.customer.customer;

import static com.aishac.customer.customer.CpfFixtures.nextValidCpf;
import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.customer.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fluxo completo do customer-service contra PostgreSQL real (spec F01-020). */
class CustomerApiIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String BASE = "/api/v1/customers";

    @LocalServerPort
    private int port;

    // CA-01
    @Test
    void shouldRegisterCustomerWithNormalizedData() {
        // Given
        String cpf = nextValidCpf();
        String email = unique("Ana") + "@Example.com ";

        // When
        ResponseEntity<String> response = createCustomer("Ana Souza", formatted(cpf), email);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode body = json(response);
        assertThat(response.getHeaders().getLocation())
                .hasToString(url(BASE + "/" + body.path("id").asString()));
        assertThat(body.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.path("cpf").asString()).isEqualTo(cpf);
        assertThat(body.path("email").asString()).isEqualTo(email.strip().toLowerCase());
        assertThat(body.path("version").asLong()).isZero();
        assertThat(UUID.fromString(body.path("id").asString()).version()).isEqualTo(7);
    }

    // CA-02
    @Test
    void shouldRejectInvalidCpf() {
        ResponseEntity<String> response = createCustomer("Ana Souza", "529.982.247-24", unique("a") + "@x.com");
        assertProblem(response, 400, "INVALID_CPF");
    }

    // CA-03
    @Test
    void shouldRejectDuplicatedCpfEvenFromInactiveCustomer() {
        // Given
        String cpf = nextValidCpf();
        String id = id(createCustomer("Ana Souza", cpf, unique("a") + "@x.com"));
        exchange(HttpMethod.POST, BASE + "/" + id + "/deactivate", null);

        // When
        ResponseEntity<String> response = createCustomer("Outra Pessoa", formatted(cpf), unique("b") + "@x.com");

        // Then
        assertProblem(response, 409, "CPF_ALREADY_EXISTS");
    }

    // CA-04
    @Test
    void shouldRejectDuplicatedEmailIgnoringCaseOnCreateAndUpdate() {
        // Given
        String email = unique("dup") + "@example.com";
        createCustomer("Ana Souza", nextValidCpf(), email);
        String otherId = id(createCustomer("Bia Lima", nextValidCpf(), unique("b") + "@x.com"));

        // Then
        assertProblem(createCustomer("Carla Dias", nextValidCpf(), email.toUpperCase()), 409, "EMAIL_ALREADY_EXISTS");
        assertProblem(
                exchange(
                        HttpMethod.PUT,
                        BASE + "/" + otherId,
                        Map.of("fullName", "Bia Lima", "email", email.toUpperCase(), "version", 0)),
                409,
                "EMAIL_ALREADY_EXISTS");
    }

    // CA-06
    @Test
    void shouldGetCustomerByIdOrAnswerNotFound() {
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));

        assertThat(json(exchange(HttpMethod.GET, BASE + "/" + id, null))
                        .path("id")
                        .asString())
                .isEqualTo(id);
        assertProblem(exchange(HttpMethod.GET, BASE + "/" + UUID.randomUUID(), null), 404, "CUSTOMER_NOT_FOUND");
    }

    // CA-07
    @Test
    void shouldFilterAndPaginate() {
        // Given
        String token = unique("Lote");
        for (int i = 0; i < 25; i++) {
            String name = (i < 3 ? "Maria Souza " : "Cliente ") + token + " " + i;
            String id = id(createCustomer(name, nextValidCpf(), unique("p" + i) + "@x.com"));
            if (i == 5) {
                exchange(HttpMethod.POST, BASE + "/" + id + "/block", Map.of("reason", "Suspeita de fraude"));
            }
        }

        // When
        JsonNode firstPage =
                json(exchange(HttpMethod.GET, BASE + "?name=" + token + "&size=10&sort=fullName,asc", null));
        JsonNode souzas = json(exchange(HttpMethod.GET, BASE + "?name=SOUZA " + token, null));
        JsonNode blocked = json(exchange(HttpMethod.GET, BASE + "?name=" + token + "&status=BLOCKED", null));

        // Then
        assertThat(firstPage.path("content")).hasSize(10);
        assertThat(firstPage.path("page").path("totalElements").asLong()).isEqualTo(25);
        assertThat(firstPage.path("page").path("totalPages").asInt()).isEqualTo(3);
        assertThat(firstPage.path("content").get(0).path("fullName").asString()).startsWith("Cliente");
        assertThat(souzas.path("content")).hasSize(3);
        assertThat(blocked.path("content")).hasSize(1);
        assertProblem(exchange(HttpMethod.GET, BASE + "?size=101", null), 400, "VALIDATION_ERROR");
        assertProblem(exchange(HttpMethod.GET, BASE + "?sort=cpf,asc", null), 400, "VALIDATION_ERROR");
    }

    // CA-08
    @Test
    void shouldFindCustomerByCpfOrEmailForCheckout() {
        // Given
        String cpf = nextValidCpf();
        String email = unique("checkout") + "@example.com";
        String id = id(createCustomer("Ana Souza", cpf, email));

        // Then
        assertThat(json(exchange(HttpMethod.GET, BASE + "?cpf=" + formatted(cpf), null))
                        .path("content")
                        .get(0)
                        .path("id")
                        .asString())
                .isEqualTo(id);
        assertThat(json(exchange(HttpMethod.GET, BASE + "?email=" + email.toUpperCase(), null))
                        .path("content")
                        .get(0)
                        .path("id")
                        .asString())
                .isEqualTo(id);
        assertThat(json(exchange(HttpMethod.GET, BASE + "?cpf=" + nextValidCpf(), null))
                        .path("content"))
                .isEmpty();
    }

    // CA-09, CA-10
    @Test
    void shouldUpdateCustomerAndRejectStaleVersion() {
        // Given
        String cpf = nextValidCpf();
        String id = id(createCustomer("Ana Souza", cpf, unique("a") + "@x.com"));
        Map<String, Object> update =
                Map.of("fullName", "Ana Souza Lima", "email", unique("lima") + "@x.com", "version", 0);

        // When
        JsonNode updated = json(exchange(HttpMethod.PUT, BASE + "/" + id, update));

        // Then
        assertThat(updated.path("fullName").asString()).isEqualTo("Ana Souza Lima");
        assertThat(updated.path("cpf").asString()).isEqualTo(cpf);
        assertThat(updated.path("version").asLong()).isEqualTo(1);
        assertProblem(exchange(HttpMethod.PUT, BASE + "/" + id, update), 409, "CONCURRENT_MODIFICATION");
    }

    // CA-11, CA-12, CA-13, CA-14
    @Test
    void shouldWalkThroughStatusLifecycle() {
        // Given
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));

        // When / Then
        JsonNode blocked = json(exchange(HttpMethod.POST, BASE + "/" + id + "/block", Map.of("reason", "Fraude")));
        assertThat(blocked.path("status").asString()).isEqualTo("BLOCKED");
        assertThat(blocked.path("blockReason").asString()).isEqualTo("Fraude");

        JsonNode inactive = json(exchange(HttpMethod.POST, BASE + "/" + id + "/deactivate", null));
        assertThat(inactive.path("status").asString()).isEqualTo("INACTIVE");
        assertThat(inactive.path("blockReason").isNull()).isTrue();

        assertThat(json(exchange(HttpMethod.GET, BASE + "/" + id, null))
                        .path("status")
                        .asString())
                .isEqualTo("INACTIVE");
        assertProblem(exchange(HttpMethod.POST, BASE + "/" + id + "/unblock", null), 409, "INVALID_STATUS_TRANSITION");
        assertProblem(
                exchange(HttpMethod.POST, BASE + "/" + id + "/addresses", address("Casa", "SP")),
                422,
                "CUSTOMER_INACTIVE");
    }

    // CA-15, CA-16, CA-17
    @Test
    void shouldManageAddressesKeepingSingleDefault() {
        // Given
        String customer = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));

        // When
        ResponseEntity<String> created = exchange(HttpMethod.POST, customer + "/addresses", address("A", "SP"));
        String a = id(created);
        String b = id(exchange(HttpMethod.POST, customer + "/addresses", address("B", "RJ")));
        String c = id(exchange(HttpMethod.POST, customer + "/addresses", address("C", "MG")));

        // Then
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(json(created).path("isDefault").asBoolean()).isTrue();
        assertThat(json(created).path("zipCode").asString()).isEqualTo("01310100");
        assertThat(json(created).path("country").asString()).isEqualTo("BR");

        assertThat(exchange(HttpMethod.PUT, customer + "/addresses/" + b + "/default", null)
                        .getStatusCode()
                        .value())
                .isEqualTo(204);
        assertThat(defaultAddressId(customer)).isEqualTo(b);

        assertThat(exchange(HttpMethod.DELETE, customer + "/addresses/" + b, null)
                        .getStatusCode()
                        .value())
                .isEqualTo(204);
        assertThat(defaultAddressId(customer)).isEqualTo(c);

        JsonNode updated = json(exchange(HttpMethod.PUT, customer + "/addresses/" + a, address("A2", "BA")));
        assertThat(updated.path("label").asString()).isEqualTo("A2");
        assertThat(json(exchange(HttpMethod.GET, customer, null))
                        .path("version")
                        .asLong())
                .as("cada operação de endereço incrementa a version do cliente: 3 inclusões, padrão, remoção, edição")
                .isEqualTo(6);
    }

    // CA-19
    @Test
    void shouldNotTouchAddressOfAnotherCustomer() {
        // Given
        String ana = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        String bia = BASE + "/" + id(createCustomer("Bia Lima", nextValidCpf(), unique("b") + "@x.com"));
        String anaAddress = id(exchange(HttpMethod.POST, ana + "/addresses", address("Casa", "SP")));

        // Then
        assertProblem(exchange(HttpMethod.DELETE, bia + "/addresses/" + anaAddress, null), 404, "ADDRESS_NOT_FOUND");
        assertProblem(exchange(HttpMethod.POST, ana + "/addresses", address("Casa", "XX")), 400, "VALIDATION_ERROR");
    }

    // CA-20
    @Test
    void shouldReturnOrderDataWithDefaultOrChosenAddress() {
        // Given
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        String home = id(exchange(HttpMethod.POST, BASE + "/" + id + "/addresses", address("Casa", "SP")));
        String work = id(exchange(HttpMethod.POST, BASE + "/" + id + "/addresses", address("Trabalho", "RJ")));

        // When
        JsonNode byDefault = json(exchange(HttpMethod.GET, BASE + "/" + id + "/order-data", null));
        JsonNode chosen = json(exchange(HttpMethod.GET, BASE + "/" + id + "/order-data?addressId=" + work, null));

        // Then
        assertThat(byDefault.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(byDefault.path("shippingAddress").path("id").asString()).isEqualTo(home);
        assertThat(chosen.path("shippingAddress").path("id").asString()).isEqualTo(work);
        assertProblem(
                exchange(HttpMethod.GET, BASE + "/" + id + "/order-data?addressId=" + UUID.randomUUID(), null),
                404,
                "ADDRESS_NOT_FOUND");
    }

    // CA-21
    @Test
    void shouldReturnOrderDataForBlockedCustomerWithoutAddress() {
        // Given
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        exchange(HttpMethod.POST, BASE + "/" + id + "/block", Map.of("reason", "Fraude"));

        // When
        ResponseEntity<String> response = exchange(HttpMethod.GET, BASE + "/" + id + "/order-data", null);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).path("status").asString()).isEqualTo("BLOCKED");
        assertThat(json(response).has("shippingAddress")).isTrue();
        assertThat(json(response).path("shippingAddress").isNull()).isTrue();
    }

    // --- Apoio -----------------------------------------------------------------------------------

    private ResponseEntity<String> createCustomer(String fullName, String cpf, String email) {
        return exchange(HttpMethod.POST, BASE, Map.of("fullName", fullName, "cpf", cpf, "email", email));
    }

    private static Map<String, Object> address(String label, String state) {
        return Map.of(
                "label", label,
                "zipCode", "01310-100",
                "street", "Avenida Paulista",
                "number", "1000",
                "district", "Bela Vista",
                "city", "São Paulo",
                "state", state);
    }

    private String defaultAddressId(String customer) {
        JsonNode list = json(exchange(HttpMethod.GET, customer + "/addresses", null));
        assertThat(list.get(0).path("isDefault").asBoolean()).isTrue();
        return list.get(0).path("id").asString();
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, @Nullable Object body) {
        RestClient.RequestBodySpec request =
                RestClient.create(url("")).method(method).uri(path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(JSON.writeValueAsString(body));
        }
        return request.retrieve().onStatus(status -> true, (req, res) -> {}).toEntity(String.class);
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(response).path("code").asString()).isEqualTo(code);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static String id(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(201);
        return json(response).path("id").asString();
    }

    private static JsonNode json(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody());
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String formatted(String cpf) {
        return "%s.%s.%s-%s".formatted(cpf.substring(0, 3), cpf.substring(3, 6), cpf.substring(6, 9), cpf.substring(9));
    }
}
