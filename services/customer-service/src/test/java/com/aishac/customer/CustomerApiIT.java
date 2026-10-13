package com.aishac.customer;

import static com.aishac.customer.CpfFixtures.formatted;
import static com.aishac.customer.CpfFixtures.nextValidCpf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    void shouldCreateCustomerWithNormalizedData() {
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
        assertThat(body.path("createdAt").asString()).isNotBlank();
    }

    // CA-02
    @Test
    void shouldRejectInvalidCpf() {
        assertProblem(createCustomer("Ana Souza", "529.982.247-24", unique("a") + "@x.com"), 400, "INVALID_CPF");
    }

    // CA-03
    @Test
    void shouldRejectDuplicatedCpfEvenFromInactiveCustomer() {
        String cpf = nextValidCpf();
        String id = id(createCustomer("Ana Souza", cpf, unique("a") + "@x.com"));
        exchange(HttpMethod.POST, BASE + "/" + id + "/deactivate", null);

        assertProblem(
                createCustomer("Outra Pessoa", formatted(cpf), unique("b") + "@x.com"), 409, "CPF_ALREADY_EXISTS");
    }

    // CA-04
    @Test
    void shouldRejectDuplicatedEmailIgnoringCaseOnCreateAndUpdate() {
        String email = unique("dup") + "@example.com";
        createCustomer("Ana Souza", nextValidCpf(), email);
        String otherId = id(createCustomer("Bia Lima", nextValidCpf(), unique("b") + "@x.com"));

        assertProblem(createCustomer("Carla Dias", nextValidCpf(), email.toUpperCase()), 409, "EMAIL_ALREADY_EXISTS");
        assertProblem(
                exchange(HttpMethod.PUT, BASE + "/" + otherId, update("Bia Lima", email.toUpperCase(), 0)),
                409,
                "EMAIL_ALREADY_EXISTS");
    }

    // CA-06
    @Test
    void shouldFindCustomerByIdOrAnswerNotFound() {
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));

        assertThat(json(exchange(HttpMethod.GET, BASE + "/" + id, null))
                        .path("id")
                        .asString())
                .isEqualTo(id);
        assertProblem(exchange(HttpMethod.GET, BASE + "/" + UUID.randomUUID(), null), 404, "CUSTOMER_NOT_FOUND");
    }

    // CA-07
    @Test
    void shouldFilterSortAndPaginate() {
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
        assertThat(json(exchange(HttpMethod.GET, BASE + "?size=500", null))
                        .path("page")
                        .path("size")
                        .asInt())
                .as("tamanho de página limitado a 100")
                .isEqualTo(100);
        assertProblem(exchange(HttpMethod.GET, BASE + "?sort=unknownField,asc", null), 400, "VALIDATION_ERROR");
    }

    // CA-08
    @Test
    void shouldFindCustomerByCpfOrEmailForCheckout() {
        String cpf = nextValidCpf();
        String email = unique("checkout") + "@example.com";
        String id = id(createCustomer("Ana Souza", cpf, email));

        assertThat(firstId(BASE + "?cpf=" + formatted(cpf))).isEqualTo(id);
        assertThat(firstId(BASE + "?email=" + email.toUpperCase())).isEqualTo(id);
        assertThat(json(exchange(HttpMethod.GET, BASE + "?cpf=" + nextValidCpf(), null))
                        .path("content"))
                .isEmpty();
    }

    // CA-09, CA-10
    @Test
    void shouldUpdateCustomerAndRejectStaleVersionWithoutChanges() {
        // Given
        String cpf = nextValidCpf();
        String id = id(createCustomer("Ana Souza", cpf, unique("a") + "@x.com"));
        String newEmail = unique("lima") + "@x.com";

        // When
        JsonNode updated = json(exchange(HttpMethod.PUT, BASE + "/" + id, update("Ana Souza Lima", newEmail, 0)));

        // Then
        assertThat(updated.path("fullName").asString()).isEqualTo("Ana Souza Lima");
        assertThat(updated.path("cpf").asString()).isEqualTo(cpf);
        assertThat(updated.path("version").asLong()).isEqualTo(1);

        assertProblem(
                exchange(HttpMethod.PUT, BASE + "/" + id, update("Outro Nome", unique("x") + "@x.com", 0)),
                409,
                "CONCURRENT_MODIFICATION");
        JsonNode afterConflict = json(exchange(HttpMethod.GET, BASE + "/" + id, null));
        assertThat(afterConflict.path("fullName").asString()).isEqualTo("Ana Souza Lima");
        assertThat(afterConflict.path("version").asLong()).isEqualTo(1);
    }

    // CA-11, CA-12, CA-13, CA-14
    @Test
    void shouldWalkThroughStatusLifecycleAndFreezeInactiveCustomer() {
        // Given
        String customer = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        String home = id(exchange(HttpMethod.POST, customer + "/addresses", address("Casa", "SP")));

        // When / Then
        JsonNode blocked = json(exchange(HttpMethod.POST, customer + "/block", Map.of("reason", "Fraude")));
        assertThat(blocked.path("status").asString()).isEqualTo("BLOCKED");
        assertThat(blocked.path("blockReason").asString()).isEqualTo("Fraude");

        JsonNode inactive = json(exchange(HttpMethod.POST, customer + "/deactivate", null));
        assertThat(inactive.path("status").asString()).isEqualTo("INACTIVE");
        assertThat(inactive.path("blockReason").isNull()).isTrue();
        assertThat(json(exchange(HttpMethod.GET, customer, null)).path("status").asString())
                .isEqualTo("INACTIVE");

        assertProblem(exchange(HttpMethod.POST, customer + "/unblock", null), 409, "INVALID_STATUS_TRANSITION");
        long version = inactive.path("version").asLong();
        assertProblem(exchange(HttpMethod.PUT, customer, update("Ana", "z@x.com", version)), 422, "CUSTOMER_INACTIVE");
        assertProblem(
                exchange(HttpMethod.POST, customer + "/addresses", address("Nova", "SP")), 422, "CUSTOMER_INACTIVE");
        assertProblem(
                exchange(HttpMethod.PUT, customer + "/addresses/" + home, address("Casa 2", "SP")),
                422,
                "CUSTOMER_INACTIVE");
        assertProblem(exchange(HttpMethod.DELETE, customer + "/addresses/" + home, null), 422, "CUSTOMER_INACTIVE");
    }

    // CA-15, CA-16, CA-17
    @Test
    void shouldManageAddressesKeepingSingleDefault() {
        // Given
        String customer = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));

        // When: A (primeiro, padrão), B e C (mais recente)
        ResponseEntity<String> created = exchange(HttpMethod.POST, customer + "/addresses", address("A", "SP"));
        String a = id(created);
        String b = id(exchange(HttpMethod.POST, customer + "/addresses", address("B", "RJ")));
        String c = id(exchange(HttpMethod.POST, customer + "/addresses", address("C", "MG")));

        // Then (CA-15)
        assertThat(json(created).path("isDefault").asBoolean()).isTrue();
        assertThat(json(created).path("zipCode").asString()).isEqualTo("01310100");
        assertThat(json(created).path("country").asString()).isEqualTo("BR");

        // When / Then (CA-17): remover o padrão A promove o mais recente, C
        assertThat(exchange(HttpMethod.DELETE, customer + "/addresses/" + a, null)
                        .getStatusCode()
                        .value())
                .isEqualTo(204);
        assertThat(defaultAddressId(customer)).isEqualTo(c);

        // When / Then (CA-16): definir B como padrão desmarca C
        assertThat(exchange(HttpMethod.PUT, customer + "/addresses/" + b + "/default", null)
                        .getStatusCode()
                        .value())
                .isEqualTo(204);
        assertThat(defaultAddressId(customer)).isEqualTo(b);

        assertThat(json(exchange(HttpMethod.PUT, customer + "/addresses/" + c, address("C2", "BA")))
                        .path("label")
                        .asString())
                .isEqualTo("C2");
        assertThat(json(exchange(HttpMethod.GET, customer, null))
                        .path("version")
                        .asLong())
                .as("cada operação de endereço incrementa a version do cliente: 3 inclusões, remoção, padrão, edição")
                .isEqualTo(6);
    }

    // CA-10: uma operação de endereço invalida a version lida antes dela
    @Test
    void shouldRejectUpdateWithVersionReadBeforeAnAddressChange() {
        // Given
        String customer = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        long versionReadBefore =
                json(exchange(HttpMethod.GET, customer, null)).path("version").asLong();
        id(exchange(HttpMethod.POST, customer + "/addresses", address("Casa", "SP")));

        // When
        ResponseEntity<String> response =
                exchange(HttpMethod.PUT, customer, update("Ana Lima", unique("x") + "@x.com", versionReadBefore));

        // Then
        assertProblem(response, 409, "CONCURRENT_MODIFICATION");
    }

    // CA-18
    @Test
    void shouldRejectEleventhAddress() {
        String customer = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        for (int i = 0; i < 10; i++) {
            id(exchange(HttpMethod.POST, customer + "/addresses", address("E" + i, "SP")));
        }

        assertProblem(
                exchange(HttpMethod.POST, customer + "/addresses", address("Extra", "SP")),
                422,
                "ADDRESS_LIMIT_EXCEEDED");
    }

    // CA-19
    @Test
    void shouldNotTouchAddressOfAnotherCustomer() {
        String ana = BASE + "/" + id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        String bia = BASE + "/" + id(createCustomer("Bia Lima", nextValidCpf(), unique("b") + "@x.com"));
        String anaAddress = id(exchange(HttpMethod.POST, ana + "/addresses", address("Casa", "SP")));

        assertProblem(exchange(HttpMethod.DELETE, bia + "/addresses/" + anaAddress, null), 404, "ADDRESS_NOT_FOUND");
        assertProblem(exchange(HttpMethod.POST, ana + "/addresses", address("Casa", "XX")), 400, "VALIDATION_ERROR");
    }

    // CA-20
    @Test
    void shouldReturnOrderDataWithDefaultOrChosenAddress() {
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        String home = id(exchange(HttpMethod.POST, BASE + "/" + id + "/addresses", address("Casa", "SP")));
        String work = id(exchange(HttpMethod.POST, BASE + "/" + id + "/addresses", address("Trabalho", "RJ")));

        JsonNode byDefault = json(exchange(HttpMethod.GET, BASE + "/" + id + "/order-data", null));
        JsonNode chosen = json(exchange(HttpMethod.GET, BASE + "/" + id + "/order-data?addressId=" + work, null));

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
        String id = id(createCustomer("Ana Souza", nextValidCpf(), unique("a") + "@x.com"));
        exchange(HttpMethod.POST, BASE + "/" + id + "/block", Map.of("reason", "Fraude"));

        ResponseEntity<String> response = exchange(HttpMethod.GET, BASE + "/" + id + "/order-data", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).path("status").asString()).isEqualTo("BLOCKED");
        assertThat(json(response).has("shippingAddress")).isTrue();
        assertThat(json(response).path("shippingAddress").isNull()).isTrue();
    }

    // Banco e Java contam caracteres de jeitos diferentes: o CHECK vira 400, nunca 500
    @Test
    void shouldAnswerBadRequestWhenOnlyTheDatabaseCheckRejectsTheData() {
        assertProblem(createCustomer("😀😀", nextValidCpf(), unique("emoji") + "@x.com"), 400, "VALIDATION_ERROR");
    }

    // --- Apoio -----------------------------------------------------------------------------------

    private ResponseEntity<String> createCustomer(String fullName, String cpf, String email) {
        return exchange(HttpMethod.POST, BASE, Map.of("fullName", fullName, "cpf", cpf, "email", email));
    }

    private static Map<String, Object> update(String fullName, String email, long version) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", fullName);
        body.put("email", email);
        body.put("version", version);
        return body;
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

    private String firstId(String path) {
        return json(exchange(HttpMethod.GET, path, null))
                .path("content")
                .get(0)
                .path("id")
                .asString();
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Object body) {
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
}
