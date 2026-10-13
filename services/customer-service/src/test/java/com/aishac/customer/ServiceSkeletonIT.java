package com.aishac.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.customer.config.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Comportamento transversal do serviço, exercitado por um controller de teste em /api/v1/probe. */
@ExtendWith(OutputCaptureExtension.class)
@Import(ServiceSkeletonIT.ProbeController.class)
class ServiceSkeletonIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Test
    void shouldReportReadinessUp() {
        ResponseEntity<String> response = get("/actuator/health/readiness", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).path("status").asString()).isEqualTo("UP");
    }

    @Test
    void shouldEchoCorrelationIdAndMaskPersonalDataInJsonLogs(CapturedOutput output) {
        // When
        ResponseEntity<String> response = get("/api/v1/probe", "test-correlation-1");

        // Then
        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo("test-correlation-1");
        assertThat(output.getOut())
                .contains("\"correlationId\":\"test-correlation-1\"")
                .contains("***.***.***-09")
                .contains("j***@example.com")
                .doesNotContain("123.456.789-09")
                .doesNotContain("joao.silva@example.com");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsUnsafe() {
        ResponseEntity<String> response = get("/api/v1/probe", "bad value\r\ninjected");

        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    void shouldAnswerUnknownRouteAsProblemDetail() {
        // When
        ResponseEntity<String> response = get("/api/v1/does-not-exist", "test-correlation-2");

        // Then
        assertProblem(response, 404, "ROUTE_NOT_FOUND");
        assertThat(json(response).path("correlationId").asString()).isEqualTo("test-correlation-2");
        assertThat(json(response).path("timestamp").isMissingNode()).isFalse();
    }

    @Test
    void shouldAnswerValidationErrorsWithFieldList() {
        // When
        ResponseEntity<String> response = RestClient.create("http://localhost:" + port)
                .post()
                .uri("/api/v1/probe")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"\"}")
                .retrieve()
                .onStatus(status -> true, (request, resp) -> {})
                .toEntity(String.class);

        // Then
        assertProblem(response, 400, "VALIDATION_ERROR");
        assertThat(json(response).path("errors").get(0).path("field").asString())
                .isEqualTo("name");
    }

    @Test
    void shouldNotLogRejectedValuesOfInvalidFields(CapturedOutput output) {
        // Given: CPF com separador errado e telefone curto escapam da máscara de logs
        String body = """
                {"fullName":"Maria Rejeitada Souza","cpf":"529.982.247/25","email":"maria rejeitada@x.com","phone":"119876"}
                """;

        // When
        ResponseEntity<String> response = RestClient.create("http://localhost:" + port)
                .post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (request, resp) -> {})
                .toEntity(String.class);

        // Then
        assertProblem(response, 400, "VALIDATION_ERROR");
        assertThat(output.getOut())
                .contains("cpf:Pattern")
                .doesNotContain("529.982.247")
                .doesNotContain("Maria Rejeitada")
                .doesNotContain("rejeitada@x.com")
                .doesNotContain("119876");
    }

    @Test
    void shouldHideInternalDetailsOnUnexpectedError() {
        ResponseEntity<String> response = get("/api/v1/probe/failure", null);

        assertProblem(response, 500, "INTERNAL_ERROR");
        assertThat(response.getBody()).doesNotContain("internal detail");
    }

    @Test
    void shouldPublishOpenApiEquivalentToTheContract() {
        // When
        JsonNode docs = json(get("/v3/api-docs", null));
        JsonNode paths = docs.path("paths");

        // Then: operações com os operationId do contrato (specs/customer/F01-020-.../contract.yaml)
        assertThat(docs.path("info").path("title").asString()).isEqualTo("Customer Service API");
        assertThat(paths.toString())
                .contains(
                        "\"listCustomers\"",
                        "\"createCustomer\"",
                        "\"getCustomer\"",
                        "\"updateCustomer\"",
                        "\"blockCustomer\"",
                        "\"unblockCustomer\"",
                        "\"deactivateCustomer\"",
                        "\"addAddress\"",
                        "\"listAddresses\"",
                        "\"updateAddress\"",
                        "\"removeAddress\"",
                        "\"setDefaultAddress\"",
                        "\"getOrderData\"");
        assertThat(paths.has("/api/v1/customers/{customerId}")).isTrue();
        assertThat(paths.has("/api/v1/customers/{customerId}/order-data")).isTrue();

        // Then: cabeçalho de correlação e erros em application/problem+json com o schema do projeto
        JsonNode getCustomer = paths.path("/api/v1/customers/{customerId}").path("get");
        assertThat(getCustomer.path("parameters").toString()).contains(CorrelationIdFilter.HEADER);
        assertThat(getCustomer.path("responses").path("404").path("content").has("application/problem+json"))
                .isTrue();
        JsonNode schemas = docs.path("components").path("schemas");
        assertThat(schemas.has("CreateCustomerRequest")).isTrue();
        assertThat(schemas.has("UpdateCustomerRequest")).isTrue();
        assertThat(schemas.has("CustomerPage")).isTrue();
        assertThat(schemas.path("ProblemDetail").path("required").toString())
                .contains("code", "correlationId", "timestamp");
    }

    @Test
    void shouldPublishOperationsAndSchemasAsDeclaredInTheContract() throws Exception {
        // Given
        JsonNode contract = contract();
        JsonNode docs = json(get("/v3/api-docs", null));
        SoftAssertions softly = new SoftAssertions();

        // Then: cada operação do contrato existe, com o mesmo operationId e os mesmos códigos de resposta
        contract.path("paths")
                .properties()
                .forEach(path -> path.getValue().properties().forEach(method -> {
                    String where = method.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey();
                    JsonNode generated = docs.path("paths").path(path.getKey()).path(method.getKey());
                    softly.assertThat(generated.path("operationId").asString())
                            .as(where + " operationId")
                            .isEqualTo(method.getValue().path("operationId").asString());
                    softly.assertThat(names(generated.path("responses")))
                            .as(where + " responses")
                            .isEqualTo(names(method.getValue().path("responses")));
                }));

        // Then: cada schema do contrato existe, com os mesmos obrigatórios, enums, $ref e campos anuláveis
        JsonNode generatedSchemas = docs.path("components").path("schemas");
        contract.path("components").path("schemas").properties().forEach(schema -> {
            String name = schema.getKey();
            JsonNode expected = schema.getValue();
            JsonNode generated = generatedSchemas.path(name);
            softly.assertThat(generated.isMissingNode()).as(name + " exists").isFalse();
            softly.assertThat(values(generated.path("required")))
                    .as(name + " required")
                    .isEqualTo(values(expected.path("required")));
            softly.assertThat(values(generated.path("enum")))
                    .as(name + " enum")
                    .isEqualTo(values(expected.path("enum")));
            expected.path("properties").properties().forEach(property -> {
                String where = name + "." + property.getKey();
                JsonNode generatedProperty = generated.path("properties").path(property.getKey());
                softly.assertThat(generatedProperty.isMissingNode())
                        .as(where + " exists")
                        .isFalse();
                property.getValue()
                        .findValues("$ref")
                        .forEach(ref -> softly.assertThat(generatedProperty.toString())
                                .as(where + " $ref")
                                .contains(ref.asString()));
                if (property.getValue().path("type").toString().contains("\"null\"")
                        || property.getValue().path("oneOf").toString().contains("\"null\"")) {
                    softly.assertThat(generatedProperty.toString())
                            .as(where + " nullable")
                            .contains("\"null\"");
                }
            });
        });
        softly.assertThat(docs.at("/paths/~1api~1v1~1customers~1{customerId}~1addresses/get/responses/200/content"
                                + "/application~1json/schema/maxItems")
                        .asInt())
                .as("listAddresses maxItems")
                .isEqualTo(10);
        softly.assertThat(values(generatedSchemas.at("/AddressResponse/properties/country/enum")))
                .as("AddressResponse.country")
                .containsExactly("BR");
        softly.assertAll();
    }

    /** O contrato aprovado da spec (fonte da verdade da API), convertido para a mesma árvore JSON do springdoc. */
    private static JsonNode contract() throws Exception {
        Path file = Path.of("../../specs/customer/F01-020-customer-registration/contract.yaml");
        try (Reader reader = Files.newBufferedReader(file)) {
            Object yaml = new Yaml().load(reader);
            return JSON.valueToTree(yaml);
        }
    }

    private static Set<String> names(JsonNode node) {
        return new TreeSet<>(node.propertyNames());
    }

    private static Set<String> values(JsonNode node) {
        Set<String> values = new TreeSet<>();
        node.values().forEach(value -> values.add(value.asString()));
        return values;
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(response).path("code").asString()).isEqualTo(code);
    }

    private ResponseEntity<String> get(String path, String correlationId) {
        return RestClient.create("http://localhost:" + port)
                .get()
                .uri(path)
                .headers(headers -> {
                    if (correlationId != null) {
                        headers.set(CorrelationIdFilter.HEADER, correlationId);
                    }
                })
                .retrieve()
                .onStatus(status -> true, (request, response) -> {})
                .toEntity(String.class);
    }

    private static JsonNode json(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody());
    }

    record ProbeRequest(@NotBlank String name) {}

    @Slf4j
    @RestController
    @RequestMapping("/api/v1/probe")
    static class ProbeController {

        @GetMapping
        String probe() {
            log.info("Probe for customer 123.456.789-09 joao.silva@example.com");
            return "ok";
        }

        @PostMapping
        String validate(@Valid @RequestBody ProbeRequest request) {
            return request.name();
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException("internal detail");
        }
    }
}
