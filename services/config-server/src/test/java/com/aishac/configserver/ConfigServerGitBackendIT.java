package com.aishac.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/** Perfil git: lê config-repo/ de um repositório Git (aqui, um repositório local temporário na branch main). */
@ActiveProfiles("git")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConfigServerGitBackendIT {

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void gitRepository(DynamicPropertyRegistry registry) throws IOException, GitAPIException {
        Path repository = Files.createTempDirectory("config-git-");
        Files.createDirectories(repository.resolve("config-repo"));
        Files.writeString(repository.resolve("config-repo/sales-service-dev.properties"), "server.port=8082\n");
        try (Git git = Git.init()
                .setDirectory(repository.toFile())
                .setInitialBranch("main")
                .call()) {
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("config")
                    .setAuthor("test", "test@aishac.local")
                    .setCommitter("test", "test@aishac.local")
                    .setSign(false)
                    .call();
        }
        // Caminho sem codificação de URI: com Path.toUri(), espaços viram %20 e o Config Server
        // rejeita o repositório local como "fora da raiz" (ex.: perfil do Windows com espaço no nome).
        String uri = "file:///"
                + repository.toRealPath().toString().replace('\\', '/').replaceFirst("^/", "");
        registry.add("CONFIG_GIT_URI", () -> uri);
    }

    @Test
    void shouldServeConfigurationFromConfigRepoFolderOfMainBranch() {
        // When
        Map<String, Object> environment = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/sales-service/dev")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        // Then
        // version = hash do commit servido: a configuração veio do Git, não do disco.
        assertThat(environment.get("version")).asString().matches("[0-9a-f]{40}");
        @SuppressWarnings("unchecked")
        var sources = (List<Map<String, Object>>) environment.get("propertySources");
        assertThat(sources)
                .extracting(source -> (String) source.get("name"))
                .anyMatch(name -> name.endsWith("config-repo/sales-service-dev.properties"));
    }
}
