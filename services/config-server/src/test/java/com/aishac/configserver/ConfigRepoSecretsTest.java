package com.aishac.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Garante que o config-repo nunca versione um segredo: chaves sensíveis só aceitam ${VARIAVEL}. */
class ConfigRepoSecretsTest {

    private static final Path CONFIG_REPO = Path.of("../../config-repo");
    private static final Pattern SENSITIVE_KEY =
            Pattern.compile("(?i).*(password|secret|token|access-key|credential).*");
    private static final Pattern ENV_REFERENCE = Pattern.compile("\\$\\{[A-Z0-9_]+}");

    @Test
    void shouldReferenceEnvironmentVariablesForEverySensitiveProperty() throws IOException {
        // Given
        List<Path> files = propertiesFiles();

        // When
        List<String> literalSecrets = new ArrayList<>();
        for (Path file : files) {
            Properties properties = load(file);
            properties.stringPropertyNames().stream()
                    .filter(key -> SENSITIVE_KEY.matcher(key).matches())
                    .filter(key ->
                            !ENV_REFERENCE.matcher(properties.getProperty(key)).matches())
                    .forEach(key -> literalSecrets.add(file.getFileName() + " -> " + key));
        }

        // Then
        assertThat(files).isNotEmpty();
        assertThat(literalSecrets).isEmpty();
    }

    private static List<Path> propertiesFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(CONFIG_REPO)) {
            return paths.filter(path -> path.toString().endsWith(".properties")).toList();
        }
    }

    private static Properties load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties;
    }
}
