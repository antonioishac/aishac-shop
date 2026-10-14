package com.aishac.sales.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.sales.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fluxo completo do catálogo contra PostgreSQL real (spec F01-030). O seed do perfil local também está no banco. */
class CatalogApiIT extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CATEGORIES = "/api/v1/categories";
    private static final String PRODUCTS = "/api/v1/products";

    @LocalServerPort
    private int port;

    @Autowired
    private InMemoryProductImageStorage storage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreStorage() {
        storage.setAvailable(true);
        storage.setPresignAvailable(true);
    }

    // ---- categorias ------------------------------------------------------------------------------

    // CA-01
    @Test
    void shouldCreateActiveCategory() {
        // When
        ResponseEntity<String> response = createCategory(unique("Calçados"));

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode body = json(response);
        assertThat(response.getHeaders().getLocation())
                .hasToString(url(CATEGORIES + "/" + body.path("id").asString()));
        assertThat(body.path("active").asBoolean()).isTrue();
        assertThat(body.path("version").asLong()).isZero();
        assertThat(UUID.fromString(body.path("id").asString()).version()).isEqualTo(7);
    }

    // CA-02
    @Test
    void shouldRejectDuplicatedCategoryNameIgnoringCaseEvenWhenInactive() {
        // Given
        String name = unique("Calçados");
        String id = id(createCategory(name));
        exchange(HttpMethod.POST, CATEGORIES + "/" + id + "/deactivate", null);
        String otherId = id(createCategory(unique("Outra")));

        // Then
        assertProblem(createCategory(name.toLowerCase(Locale.ROOT)), 409, "CATEGORY_NAME_ALREADY_EXISTS");
        assertProblem(
                exchange(
                        HttpMethod.PUT,
                        CATEGORIES + "/" + otherId,
                        Map.of("name", name.toUpperCase(Locale.ROOT), "version", 0)),
                409,
                "CATEGORY_NAME_ALREADY_EXISTS");
    }

    // CA-03
    @Test
    void shouldHideProductsOfInactiveCategoryFromSellableFilter() {
        // Given
        String categoryId = id(createCategory(unique("Vitrine")));
        String productId = id(createProduct(categoryId, sku(), "Produto Vitrine", "10.00", 1));
        String sellable = PRODUCTS + "?sellable=true&categoryId=" + categoryId;

        // When
        exchange(HttpMethod.POST, CATEGORIES + "/" + categoryId + "/deactivate", null);

        // Then
        assertThat(ids(get(sellable))).isEmpty();
        JsonNode product = json(get(PRODUCTS + "/" + productId));
        assertThat(product.path("active").asBoolean()).isTrue();
        assertThat(product.path("sellable").asBoolean()).isFalse();
        assertThat(product.path("category").path("active").asBoolean()).isFalse();

        // When
        exchange(HttpMethod.POST, CATEGORIES + "/" + categoryId + "/activate", null);

        // Then
        assertThat(ids(get(sellable))).containsExactly(productId);
    }

    // CA-04
    @Test
    void shouldListOnlyActiveCategoriesSortedByName() {
        // Given
        String prefix = unique("Lista");
        String b = id(createCategory(prefix + " B"));
        String a = id(createCategory(prefix + " A"));
        String inactive = id(createCategory(prefix + " C"));
        exchange(HttpMethod.POST, CATEGORIES + "/" + inactive + "/deactivate", null);

        // When
        JsonNode page = json(get(CATEGORIES + "?active=true&name=" + prefix));

        // Then
        assertThat(ids(page)).containsExactly(a, b);
        assertThat(page.path("page").path("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void shouldKeepDeactivationIdempotent() {
        String id = id(createCategory(unique("Idem")));

        exchange(HttpMethod.POST, CATEGORIES + "/" + id + "/deactivate", null);
        JsonNode again = json(exchange(HttpMethod.POST, CATEGORIES + "/" + id + "/deactivate", null));

        assertThat(again.path("active").asBoolean()).isFalse();
        assertThat(again.path("version").asLong()).isEqualTo(1);
    }

    @Test
    void shouldAnswerNotFoundForUnknownCategory() {
        assertProblem(get(CATEGORIES + "/" + UUID.randomUUID()), 404, "CATEGORY_NOT_FOUND");
    }

    // ---- produtos --------------------------------------------------------------------------------

    // CA-05
    @Test
    void shouldCreateProductWithUppercaseSkuAndInitialStockEntry() {
        // Given
        String categoryId = id(createCategory(unique("Calçados")));
        String sku = sku().toLowerCase(Locale.ROOT);

        // When
        ResponseEntity<String> response = createProduct(categoryId, sku, "Tênis Run", "299.90", 10);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode body = json(response);
        assertThat(response.getHeaders().getLocation())
                .hasToString(url(PRODUCTS + "/" + body.path("id").asString()));
        assertThat(body.path("sku").asString()).isEqualTo(sku.toUpperCase(Locale.ROOT));
        assertThat(body.path("price").asString()).isEqualTo("299.90");
        assertThat(body.path("stockQuantity").asInt()).isEqualTo(10);
        assertThat(body.path("active").asBoolean()).isTrue();
        assertThat(body.path("sellable").asBoolean()).isTrue();
        assertThat(body.path("version").asLong()).isZero();
        assertThat(body.path("primaryImage").isNull()).isTrue();
        JsonNode history = json(get(PRODUCTS + "/" + body.path("id").asString() + "/stock-movements"));
        assertThat(history.path("content")).hasSize(1);
        assertThat(history.path("content").get(0).path("type").asString()).isEqualTo("ENTRY");
        assertThat(history.path("content").get(0).path("resultingStock").asInt())
                .isEqualTo(10);
    }

    @Test
    void shouldFormatPriceWithTwoDecimals() {
        String categoryId = id(createCategory(unique("Preço")));

        JsonNode body = json(createProduct(categoryId, sku(), "Produto Preço", "129.9", 0));

        assertThat(body.path("price").asString()).isEqualTo("129.90");
        assertThat(json(get(PRODUCTS + "/" + body.path("id").asString() + "/stock-movements"))
                        .path("content"))
                .isEmpty();
    }

    // CA-06
    @Test
    void shouldRejectInvalidProductFields() {
        String categoryId = id(createCategory(unique("Validação")));

        for (String price : List.of("0.00", "-1", "10.999", "abc")) {
            ResponseEntity<String> response = createProduct(categoryId, sku(), "Produto Válido", price, 1);
            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(fields(response)).as(price).contains("price");
        }
        ResponseEntity<String> response = createProduct(categoryId, "SKU COM ESPACO", "Ab", "10.00", 1);
        assertProblem(response, 400, "VALIDATION_ERROR");
        assertThat(fields(response)).contains("sku", "name");
    }

    // CA-07
    @Test
    void shouldRejectDuplicatedSkuIgnoringCase() {
        String categoryId = id(createCategory(unique("SKU")));
        String sku = sku();
        createProduct(categoryId, sku, "Produto Um", "10.00", 0);

        assertProblem(
                createProduct(categoryId, sku.toLowerCase(Locale.ROOT), "Produto Dois", "10.00", 0),
                409,
                "SKU_ALREADY_EXISTS");
    }

    // CA-08
    @Test
    void shouldRejectInactiveOrUnknownCategoryOnCreateAndMove() {
        // Given
        String active = id(createCategory(unique("Ativa")));
        String inactive = id(createCategory(unique("Inativa")));
        exchange(HttpMethod.POST, CATEGORIES + "/" + inactive + "/deactivate", null);
        String productId = id(createProduct(active, sku(), "Produto Movido", "10.00", 0));
        String unknown = UUID.randomUUID().toString();

        // Then
        assertProblem(createProduct(inactive, sku(), "Produto Novo", "10.00", 0), 422, "CATEGORY_INACTIVE");
        assertProblem(createProduct(unknown, sku(), "Produto Novo", "10.00", 0), 404, "CATEGORY_NOT_FOUND");
        assertProblem(
                exchange(HttpMethod.PUT, PRODUCTS + "/" + productId, productUpdate("Produto Movido", inactive, 0)),
                422,
                "CATEGORY_INACTIVE");
        assertProblem(
                exchange(HttpMethod.PUT, PRODUCTS + "/" + productId, productUpdate("Produto Movido", unknown, 0)),
                404,
                "CATEGORY_NOT_FOUND");
    }

    // CA-09
    @Test
    void shouldUpdateProductWithOptimisticLockKeepingStock() {
        // Given
        String categoryId = id(createCategory(unique("Lock")));
        String productId = id(createProduct(categoryId, sku(), "Produto Lock", "10.00", 10));

        // When
        ResponseEntity<String> first =
                exchange(HttpMethod.PUT, PRODUCTS + "/" + productId, productUpdate("Produto Novo", categoryId, 0));
        ResponseEntity<String> second =
                exchange(HttpMethod.PUT, PRODUCTS + "/" + productId, productUpdate("Produto Outro", categoryId, 0));

        // Then
        assertThat(first.getStatusCode().value()).isEqualTo(200);
        assertThat(json(first).path("version").asLong()).isEqualTo(1);
        assertThat(json(first).path("price").asString()).isEqualTo("19.90");
        assertThat(json(first).path("stockQuantity").asInt()).isEqualTo(10);
        assertProblem(second, 409, "CONCURRENT_MODIFICATION");
        assertThat(json(get(PRODUCTS + "/" + productId)).path("name").asString())
                .isEqualTo("Produto Novo");
    }

    // CA-10
    @Test
    void shouldCombineProductFiltersAndSortByPrice() {
        // Given
        String categoryId = id(createCategory(unique("Filtro")));
        String otherCategory = id(createCategory(unique("Filtro Outra")));
        String cheap = id(createProduct(categoryId, sku(), "Tenis Barato", "150.00", 1));
        String expensive = id(createProduct(categoryId, sku(), "TENIS Caro", "280.00", 1));
        createProduct(categoryId, sku(), "Tenis Fora Da Faixa", "350.00", 1);
        createProduct(categoryId, sku(), "Camiseta", "200.00", 1);
        createProduct(otherCategory, sku(), "Tenis Outra Categoria", "200.00", 1);
        String inactive = id(createProduct(categoryId, sku(), "Tenis Inativo", "200.00", 1));
        exchange(HttpMethod.POST, PRODUCTS + "/" + inactive + "/deactivate", null);

        // When
        JsonNode page = json(get(PRODUCTS + "?categoryId=" + categoryId
                + "&name=tenis&minPrice=100.00&maxPrice=300.00&sellable=true&sort=price,asc"));

        // Then
        assertThat(ids(page)).containsExactly(cheap, expensive);
    }

    @Test
    void shouldRejectInvalidPriceFilterAndUnknownSort() {
        assertProblem(get(PRODUCTS + "?minPrice=abc"), 400, "VALIDATION_ERROR");
        assertProblem(get(PRODUCTS + "?sort=unknown,asc"), 400, "VALIDATION_ERROR");
    }

    // CA-11
    @Test
    void shouldDeactivateAndReactivateProduct() {
        String categoryId = id(createCategory(unique("Situação")));
        String productId = id(createProduct(categoryId, sku(), "Produto Situação", "10.00", 0));
        String sellable = PRODUCTS + "?sellable=true&categoryId=" + categoryId;

        exchange(HttpMethod.POST, PRODUCTS + "/" + productId + "/deactivate", null);
        assertThat(json(get(PRODUCTS + "/" + productId)).path("active").asBoolean())
                .isFalse();
        assertThat(ids(get(sellable))).isEmpty();

        exchange(HttpMethod.POST, PRODUCTS + "/" + productId + "/activate", null);
        assertThat(ids(get(sellable))).containsExactly(productId);
    }

    // ---- estoque ---------------------------------------------------------------------------------

    // CA-12
    @Test
    void shouldRegisterStockEntry() {
        String productId = newProduct(10);

        ResponseEntity<String> response = movement(productId, "ENTRY", 5, "Reposição do fornecedor");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(json(response).path("resultingStock").asInt()).isEqualTo(15);
        assertThat(json(response).path("productId").asString()).isEqualTo(productId);
        assertThat(json(get(PRODUCTS + "/" + productId)).path("stockQuantity").asInt())
                .isEqualTo(15);
    }

    // CA-13
    @Test
    void shouldRejectExitGreaterThanStock() {
        String productId = newProduct(3);

        assertProblem(movement(productId, "EXIT", 5, "Venda balcão"), 422, "INSUFFICIENT_STOCK");
        assertThat(json(get(PRODUCTS + "/" + productId)).path("stockQuantity").asInt())
                .isEqualTo(3);
    }

    // CA-15
    @Test
    void shouldListStockHistoryFromNewestToOldest() {
        String productId = newProduct(10);
        movement(productId, "EXIT", 4, "Venda balcão");
        movement(productId, "ENTRY", 2, "Devolução");

        JsonNode content =
                json(get(PRODUCTS + "/" + productId + "/stock-movements")).path("content");

        assertThat(content).hasSize(3);
        assertThat(content.get(0).path("reason").asString()).isEqualTo("Devolução");
        assertThat(content.get(0).path("resultingStock").asInt()).isEqualTo(8);
        assertThat(content.get(1).path("type").asString()).isEqualTo("EXIT");
        assertThat(content.get(1).path("quantity").asInt()).isEqualTo(4);
        assertThat(content.get(2).path("type").asString()).isEqualTo("ENTRY");
        assertThat(content.get(2).path("createdAt").asString()).isNotBlank();
    }

    @Test
    void shouldAnswerNotFoundForStockOfUnknownProduct() {
        String unknown = UUID.randomUUID().toString();

        assertProblem(movement(unknown, "ENTRY", 1, "Entrada"), 404, "PRODUCT_NOT_FOUND");
        assertProblem(get(PRODUCTS + "/" + unknown + "/stock-movements"), 404, "PRODUCT_NOT_FOUND");
    }

    // ---- imagens ---------------------------------------------------------------------------------

    // CA-16
    @Test
    void shouldUploadFirstImageAsPrimary() {
        // Given
        String productId = newProduct(1);
        byte[] content = TestImages.jpeg(200 * 1024);

        // When
        ResponseEntity<String> response = upload(productId, content, "foto.jpg", "Tênis Run azul, vista lateral");

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode body = json(response);
        assertThat(body.path("primary").asBoolean()).isTrue();
        assertThat(body.path("position").asInt()).isEqualTo(1);
        assertThat(body.path("contentType").asString()).isEqualTo("image/jpeg");
        assertThat(body.path("sizeBytes").asLong()).isEqualTo(content.length);
        assertThat(body.path("url").asString()).startsWith("http://storage.test/products/" + productId + "/");
        assertThat(storage.content(objectKey(body.path("id").asString()))).isEqualTo(content);
        assertThat(response.getHeaders().getLocation())
                .hasToString(url(PRODUCTS + "/" + productId + "/images/"
                        + body.path("id").asString()));
        assertThat(json(get(PRODUCTS + "/" + productId))
                        .path("primaryImage")
                        .path("id")
                        .asString())
                .isEqualTo(body.path("id").asString());
    }

    // CA-17
    @Test
    void shouldRejectFileThatIsNotAnImage() {
        String productId = newProduct(1);

        assertProblem(upload(productId, TestImages.text(), "foto.jpg", "Texto"), 400, "INVALID_IMAGE_TYPE");
        assertThat(json(get(PRODUCTS + "/" + productId + "/images")).size()).isZero();
    }

    // CA-18
    @Test
    void shouldRejectLargeFileAndNinthImage() {
        // Given
        String productId = newProduct(1);
        for (int i = 1; i <= 8; i++) {
            assertThat(upload(productId, TestImages.png(64), "f.png", "Imagem " + i)
                            .getStatusCode()
                            .value())
                    .isEqualTo(201);
        }

        // Then
        assertProblem(
                upload(newProduct(1), TestImages.jpeg(6 * 1024 * 1024), "f.jpg", "Grande"), 400, "IMAGE_TOO_LARGE");
        assertProblem(upload(productId, TestImages.webp(64), "f.webp", "Nona"), 422, "IMAGE_LIMIT_EXCEEDED");
    }

    // CA-19
    @Test
    void shouldReplaceFileWithNewKeyAndRemoveOldObject() {
        // Given
        String productId = newProduct(1);
        JsonNode image = json(upload(productId, TestImages.jpeg(64), "a.jpg", "Original"));
        String imageId = image.path("id").asString();
        String oldKey = objectKey(imageId);

        // When
        ResponseEntity<String> response = replace(productId, imageId, TestImages.png(128));

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.path("id").asString()).isEqualTo(imageId);
        assertThat(body.path("altText").asString()).isEqualTo("Original");
        assertThat(body.path("primary").asBoolean()).isTrue();
        assertThat(body.path("position").asInt()).isEqualTo(1);
        assertThat(body.path("contentType").asString()).isEqualTo("image/png");
        String newKey = objectKey(imageId);
        assertThat(newKey).isNotEqualTo(oldKey).endsWith(".png");
        assertThat(storage.contains(newKey)).isTrue();
        assertThat(storage.contains(oldKey)).isFalse();
    }

    // CA-20
    @Test
    void shouldSwapPositionsAndPrimaryWithoutTouchingFiles() {
        // Given
        String productId = newProduct(1);
        String a = id(upload(productId, TestImages.jpeg(64), "a.jpg", "A"));
        String b = id(upload(productId, TestImages.jpeg(64), "b.jpg", "B"));
        String keyA = objectKey(a);

        // When
        ResponseEntity<String> response = exchange(
                HttpMethod.PATCH, PRODUCTS + "/" + productId + "/images/" + b, Map.of("primary", true, "position", 1));
        exchange(HttpMethod.PATCH, PRODUCTS + "/" + productId + "/images/" + a, Map.of("altText", "A nova"));

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode images = json(get(PRODUCTS + "/" + productId + "/images"));
        assertThat(images.get(0).path("id").asString()).isEqualTo(b);
        assertThat(images.get(0).path("primary").asBoolean()).isTrue();
        assertThat(images.get(1).path("id").asString()).isEqualTo(a);
        assertThat(images.get(1).path("position").asInt()).isEqualTo(2);
        assertThat(images.get(1).path("primary").asBoolean()).isFalse();
        assertThat(images.get(1).path("altText").asString()).isEqualTo("A nova");
        assertThat(objectKey(a)).isEqualTo(keyA);
    }

    @Test
    void shouldRejectEmptyOrInvalidImagePatch() {
        String productId = newProduct(1);
        String a = id(upload(productId, TestImages.jpeg(64), "a.jpg", "A"));
        String path = PRODUCTS + "/" + productId + "/images/" + a;

        assertProblem(exchange(HttpMethod.PATCH, path, Map.of()), 400, "VALIDATION_ERROR");
        assertProblem(exchange(HttpMethod.PATCH, path, Map.of("primary", false)), 400, "VALIDATION_ERROR");
        assertProblem(exchange(HttpMethod.PATCH, path, Map.of("position", 9)), 400, "VALIDATION_ERROR");
    }

    // CA-21
    @Test
    void shouldDeletePrimaryImageAndPromoteNext() {
        // Given
        String productId = newProduct(1);
        String a = id(upload(productId, TestImages.jpeg(64), "a.jpg", "A"));
        String b = id(upload(productId, TestImages.jpeg(64), "b.jpg", "B"));
        String keyA = objectKey(a);

        // When
        ResponseEntity<String> response =
                exchange(HttpMethod.DELETE, PRODUCTS + "/" + productId + "/images/" + a, null);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(storage.contains(keyA)).isFalse();
        JsonNode remaining = json(get(PRODUCTS + "/" + productId + "/images/" + b));
        assertThat(remaining.path("primary").asBoolean()).isTrue();
        assertProblem(get(PRODUCTS + "/" + productId + "/images/" + a), 404, "IMAGE_NOT_FOUND");
    }

    // CA-23
    @Test
    void shouldAnswerServiceUnavailableWhenStorageIsDownWithoutChangingDatabase() {
        // Given
        String productId = newProduct(1);
        String a = id(upload(productId, TestImages.jpeg(64), "a.jpg", "A"));
        String keyA = objectKey(a);
        storage.setAvailable(false);

        // Then
        assertProblem(upload(productId, TestImages.jpeg(64), "b.jpg", "B"), 503, "IMAGE_STORAGE_UNAVAILABLE");
        assertProblem(replace(productId, a, TestImages.png(64)), 503, "IMAGE_STORAGE_UNAVAILABLE");
        assertThat(imageCount(productId)).isEqualTo(1);
        assertThat(objectKey(a)).isEqualTo(keyA);
        assertThat(storage.count("products/" + productId + "/")).isEqualTo(1);

        // Then: a URL pré-assinada é calculada sem o S3, então a leitura continua respondendo
        assertThat(json(get(PRODUCTS + "/" + productId))
                        .path("primaryImage")
                        .path("id")
                        .asString())
                .isEqualTo(a);
    }

    // RN-22, pergunta 6: sem URL pré-assinada, a vitrine degrada e a leitura de imagens responde 503
    @Test
    void shouldDegradeShowcaseWhenPresignedUrlCannotBeGenerated() {
        // Given
        String productId = newProduct(1);
        upload(productId, TestImages.jpeg(64), "a.jpg", "A");
        storage.setPresignAvailable(false);

        // Then
        JsonNode product = json(get(PRODUCTS + "/" + productId));
        assertThat(product.path("id").asString()).isEqualTo(productId);
        assertThat(product.path("primaryImage").isNull()).isTrue();
        assertProblem(get(PRODUCTS + "/" + productId + "/images"), 503, "IMAGE_STORAGE_UNAVAILABLE");
    }

    @Test
    void shouldAnswerFileTooLargeAboveMultipartLimit() {
        String productId = newProduct(1);

        assertProblem(upload(productId, TestImages.jpeg(11 * 1024 * 1024), "big.jpg", "Grande"), 400, "FILE_TOO_LARGE");
    }

    @Test
    void shouldRequireFileAndAltText() {
        String productId = newProduct(1);
        MultiValueMap<String, Object> onlyAltText = new LinkedMultiValueMap<>();
        onlyAltText.add("altText", "Sem arquivo");

        ResponseEntity<String> withoutFile =
                multipart(HttpMethod.POST, PRODUCTS + "/" + productId + "/images", onlyAltText);
        ResponseEntity<String> withoutAltText = upload(productId, TestImages.jpeg(64), "a.jpg", null);

        assertProblem(withoutFile, 400, "VALIDATION_ERROR");
        assertThat(fields(withoutFile)).contains("file");
        assertProblem(withoutAltText, 400, "VALIDATION_ERROR");
        assertThat(fields(withoutAltText)).contains("altText");
    }

    // ---- apoio -----------------------------------------------------------------------------------

    private String newProduct(int stock) {
        String categoryId = id(createCategory(unique("Apoio")));
        return id(createProduct(categoryId, sku(), "Produto de Apoio", "10.00", stock));
    }

    private ResponseEntity<String> createCategory(String name) {
        return exchange(HttpMethod.POST, CATEGORIES, Map.of("name", name));
    }

    private ResponseEntity<String> createProduct(String categoryId, String sku, String name, String price, int stock) {
        return exchange(
                HttpMethod.POST,
                PRODUCTS,
                Map.of("sku", sku, "name", name, "price", price, "categoryId", categoryId, "initialStock", stock));
    }

    private static Map<String, Object> productUpdate(String name, String categoryId, long version) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", "19.90");
        body.put("categoryId", categoryId);
        body.put("version", version);
        return body;
    }

    private ResponseEntity<String> movement(String productId, String type, int quantity, String reason) {
        return exchange(
                HttpMethod.POST,
                PRODUCTS + "/" + productId + "/stock-movements",
                Map.of("type", type, "quantity", quantity, "reason", reason));
    }

    private ResponseEntity<String> upload(String productId, byte[] content, String filename, String altText) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file(content, filename));
        if (altText != null) {
            parts.add("altText", altText);
        }
        return multipart(HttpMethod.POST, PRODUCTS + "/" + productId + "/images", parts);
    }

    private ResponseEntity<String> replace(String productId, String imageId, byte[] content) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file(content, "novo.bin"));
        return multipart(HttpMethod.PUT, PRODUCTS + "/" + productId + "/images/" + imageId + "/file", parts);
    }

    private static ByteArrayResource file(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private ResponseEntity<String> multipart(HttpMethod method, String path, MultiValueMap<String, Object> parts) {
        return client().method(method)
                .uri(path)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {})
                .toEntity(String.class);
    }

    private String objectKey(String imageId) {
        return jdbcTemplate.queryForObject(
                "SELECT object_key FROM product_images WHERE id = ?", String.class, UUID.fromString(imageId));
    }

    private int imageCount(String productId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM product_images WHERE product_id = ?", Integer.class, UUID.fromString(productId));
        return count == null ? 0 : count;
    }

    private ResponseEntity<String> get(String path) {
        return exchange(HttpMethod.GET, path, null);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Object body) {
        var request = client().method(method).uri(path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.retrieve()
                .onStatus(status -> true, (req, response) -> {})
                .toEntity(String.class);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(response).path("code").asString()).isEqualTo(code);
    }

    private static List<String> fields(ResponseEntity<String> response) {
        List<String> fields = new ArrayList<>();
        json(response)
                .path("errors")
                .forEach(error -> fields.add(error.path("field").asString()));
        return fields;
    }

    private static List<String> ids(ResponseEntity<String> response) {
        return ids(json(response));
    }

    private static List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.path("content").forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }

    private static String id(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(201);
        return json(response).path("id").asString();
    }

    private static JsonNode json(ResponseEntity<String> response) {
        return JSON.readTree(response.getBody());
    }

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String sku() {
        return "T-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
