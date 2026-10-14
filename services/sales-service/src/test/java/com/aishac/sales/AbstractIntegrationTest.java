package com.aishac.sales;

import com.aishac.sales.catalog.InMemoryProductImageStorage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: um único PostgreSQL (mesma versão do compose) compartilhado por todas as classes,
 * iniciado uma vez e reaproveitado junto com o contexto Spring em cache. As imagens vão para um armazenamento em
 * memória; o S3 (LocalStack) é testado à parte, no {@code S3ProductImageStorageIT}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.StorageTestConfiguration.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine");

    static {
        POSTGRES.start();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StorageTestConfiguration {

        @Bean
        @Primary
        InMemoryProductImageStorage inMemoryProductImageStorage() {
            return new InMemoryProductImageStorage();
        }
    }
}
