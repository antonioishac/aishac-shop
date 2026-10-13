package com.aishac.customer.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * O plugin do Spring Cloud Contract ignora, só com um WARNING, o contrato YAML que não consegue ler: o build fica verde
 * sem o teste e sem o stub. Este teste garante um teste gerado por contrato.
 */
class ContractGenerationTest {

    @Test
    void shouldGenerateOneTestPerSalesServiceContract() throws Exception {
        // Given
        long contracts = countContracts("contracts/sales-service");

        // When
        long generatedTests = Arrays.stream(Class.forName("com.aishac.customer.contract.Sales_serviceContractTest")
                        .getDeclaredMethods())
                .filter(method -> method.getName().startsWith("validate_"))
                .count();

        // Then
        assertThat(generatedTests).isEqualTo(contracts).isPositive();
    }

    private static long countContracts(String directory) throws IOException, URISyntaxException {
        Path path = Path.of(ContractGenerationTest.class
                .getClassLoader()
                .getResource(directory)
                .toURI());
        try (Stream<Path> files = Files.list(path)) {
            return files.filter(file -> file.toString().endsWith(".yml")).count();
        }
    }
}
