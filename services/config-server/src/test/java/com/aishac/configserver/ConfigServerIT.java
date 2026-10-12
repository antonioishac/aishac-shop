package com.aishac.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Sobe o Config Server lendo o config-repo real do repositório. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConfigServerIT {

    @LocalServerPort
    private int port;

    @ParameterizedTest
    @CsvSource({
        "customer-service, local", "customer-service, dev", "customer-service, prod",
        "sales-service, local", "sales-service, dev", "sales-service, prod",
        "web-bff, local", "web-bff, dev", "web-bff, prod"
    })
    void shouldServeServiceAndCommonConfigurationForEveryProfile(String service, String profile) {
        // When
        Map<String, Object> environment = client().get()
                .uri("/{service}/{profile}", service, profile)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        // Then
        assertThat(sourceNames(environment))
                .anyMatch(name -> name.endsWith("/" + service + ".properties"))
                .anyMatch(name -> name.endsWith("/application-" + profile + ".properties"))
                .anyMatch(name -> name.endsWith("/application.properties"));
    }

    @Test
    void shouldServeProfileSpecificDatasourceForServicesWithDatabase() {
        // When
        Map<String, Object> environment =
                client().get().uri("/sales-service/prod").retrieve().body(new ParameterizedTypeReference<>() {});

        // Then
        assertThat(sourceNames(environment)).anyMatch(name -> name.endsWith("/sales-service-prod.properties"));
    }

    @Test
    void shouldExposeReadinessProbe() {
        // When
        String body =
                client().get().uri("/actuator/health/readiness").retrieve().body(String.class);

        // Then
        assertThat(body).contains("\"status\":\"UP\"");
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @SuppressWarnings("unchecked")
    private static List<String> sourceNames(Map<String, Object> environment) {
        var sources = (List<Map<String, Object>>) environment.get("propertySources");
        return sources.stream().map(source -> (String) source.get("name")).toList();
    }
}
