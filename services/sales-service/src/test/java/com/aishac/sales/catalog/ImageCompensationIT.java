package com.aishac.sales.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.aishac.sales.AbstractIntegrationTest;
import com.aishac.sales.catalog.entity.ProductImage;
import com.aishac.sales.catalog.repository.ProductImageRepository;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * RN-21, CA-22: o banco falha no COMMIT real (UNIQUE adiável de posição) depois de o arquivo já estar no
 * armazenamento, e a compensação remove o objeto.
 *
 * <p>Exceção consciente à rule de testes (não espionar repositório em {@code *IT}): o espião só injeta a falha e
 * sempre grava de verdade ({@code persist} + {@code flush}, como o {@code saveAndFlush} de uma imagem nova). Fica numa
 * classe própria para não alterar o caminho de gravação dos demais testes.
 */
class ImageCompensationIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private InMemoryProductImageStorage storage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Proxy compartilhado: dentro da requisição, usa a transação aberta pelo service. */
    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private ProductImageRepository productImageRepository;

    @Test
    void shouldRemoveUploadedObjectWhenCommitFails() {
        // Given: na mesma transação, outra linha ocupa a posição gravada; a UNIQUE adiável falha no COMMIT
        String productId = newProduct();
        doAnswer(invocation -> {
                    ProductImage saved = invocation.getArgument(0);
                    entityManager.persist(saved);
                    entityManager.flush();
                    jdbcTemplate.update(
                            "INSERT INTO product_images (id, product_id, object_key, content_type, size_bytes,"
                                    + " alt_text, position, is_primary, created_at, updated_at)"
                                    + " VALUES (?, ?, ?, 'image/jpeg', 1, 'Conflito', ?, false, now(), now())",
                            UUID.randomUUID(),
                            saved.getProduct().getId(),
                            "conflict/" + UUID.randomUUID(),
                            saved.getPosition());
                    return saved;
                })
                .when(productImageRepository)
                .saveAndFlush(any());

        // When
        ResponseEntity<String> response = upload(productId);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM product_images WHERE product_id = ?", Integer.class, UUID.fromString(productId));
        assertThat(rows).isZero();
        assertThat(storage.count("products/" + productId + "/")).isZero();
    }

    private String newProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String categoryId = id(post("/api/v1/categories", Map.of("name", "Compensação " + suffix)));
        return id(post(
                "/api/v1/products",
                Map.of(
                        "sku",
                        "CMP-" + suffix.toUpperCase(),
                        "name",
                        "Produto compensação",
                        "price",
                        "10.00",
                        "categoryId",
                        categoryId,
                        "initialStock",
                        0)));
    }

    private ResponseEntity<String> upload(String productId) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new ByteArrayResource(TestImages.jpeg(64)) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        });
        parts.add("altText", "Conflito");
        return client().post()
                .uri("/api/v1/products/" + productId + "/images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {})
                .toEntity(String.class);
    }

    private ResponseEntity<String> post(String path, Object body) {
        return client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {})
                .toEntity(String.class);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private static String id(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(201);
        return JSON.readTree(response.getBody()).path("id").asString();
    }
}
