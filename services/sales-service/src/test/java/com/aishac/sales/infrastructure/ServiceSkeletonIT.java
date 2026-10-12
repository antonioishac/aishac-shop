package com.aishac.sales.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.sales.AbstractIntegrationTest;
import com.aishac.sales.infrastructure.web.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Comportamento transversal do esqueleto, exercitado por um controller de teste em /api/v1/probe. */
@ExtendWith(OutputCaptureExtension.class)
@Import(ServiceSkeletonIT.ProbeController.class)
class ServiceSkeletonIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Test
    void shouldReportReadinessUp() {
        // When
        ResponseEntity<String> response = get("/actuator/health/readiness", null);

        // Then
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
    void shouldGenerateCorrelationIdWhenHeaderIsMissingOrUnsafe() {
        // When
        ResponseEntity<String> response = get("/api/v1/probe", "bad value\r\ninjected");

        // Then
        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    void shouldAnswerNotFoundAsProblemDetail() {
        // When
        ResponseEntity<String> response = get("/api/v1/does-not-exist", "test-correlation-2");

        // Then
        assertProblem(response, 404, "NOT_FOUND");
        assertThat(json(response).path("correlationId").asString()).isEqualTo("test-correlation-2");
        assertThat(json(response).path("timestamp").isMissingNode()).isFalse();
    }

    @Test
    void shouldRejectUnsupportedApiVersion() {
        // When
        ResponseEntity<String> response = get("/api/v2/probe", null);

        // Then
        assertProblem(response, 400, "BAD_REQUEST");
    }

    @Test
    void shouldAnswerValidationErrorsWithFieldList() {
        // When
        ResponseEntity<String> response = client().post()
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
    void shouldHideInternalDetailsOnUnexpectedError() {
        // When
        ResponseEntity<String> response = get("/api/v1/probe/failure", null);

        // Then
        assertProblem(response, 500, "INTERNAL_ERROR");
        assertThat(response.getBody()).doesNotContain("internal detail");
    }

    @Test
    void shouldPublishOpenApiContract() {
        // When
        ResponseEntity<String> response = get("/v3/api-docs", null);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).path("info").path("title").asString()).isEqualTo("Sales Service API");
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(response).path("code").asString()).isEqualTo(code);
    }

    private ResponseEntity<String> get(String path, String correlationId) {
        return client().get()
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

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private static JsonNode json(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody());
    }

    record ProbeRequest(@NotBlank String name) {}

    @Slf4j
    @RestController
    @RequestMapping("/api/{version}/probe")
    static class ProbeController {

        @GetMapping(version = "1")
        String probe() {
            log.info("Probe for order of customer 123.456.789-09 joao.silva@example.com");
            return "ok";
        }

        @PostMapping(version = "1")
        String validate(@Valid @RequestBody ProbeRequest request) {
            return request.name();
        }

        @GetMapping(path = "/failure", version = "1")
        String failure() {
            throw new IllegalStateException("internal detail");
        }
    }
}
