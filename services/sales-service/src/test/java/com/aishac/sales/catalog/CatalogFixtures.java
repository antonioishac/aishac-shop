package com.aishac.sales.catalog;

import com.aishac.sales.catalog.entity.Category;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.ProductImage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Entidades do catálogo prontas para os testes unitários (ids fixos, versão 0). */
public final class CatalogFixtures {

    public static final UUID CATEGORY_ID = UUID.fromString("01900000-0000-7000-8000-0000000000aa");
    public static final UUID PRODUCT_ID = UUID.fromString("01900000-0000-7000-8000-0000000000bb");
    public static final Instant NOW = Instant.parse("2026-10-13T15:00:00Z");

    private CatalogFixtures() {}

    public static Category category(boolean active) {
        return category(CATEGORY_ID, active);
    }

    public static Category category(UUID id, boolean active) {
        Category category =
                Category.builder().name("Calçados").active(active).version(0L).build();
        category.setId(id);
        category.setCreatedAt(NOW);
        category.setUpdatedAt(NOW);
        return category;
    }

    public static Product product(Category category, int stock) {
        Product product = Product.builder()
                .sku("TENIS-RUN-42")
                .name("Tênis Run")
                .price(new BigDecimal("299.90"))
                .category(category)
                .stockQuantity(stock)
                .version(0L)
                .build();
        product.setId(PRODUCT_ID);
        product.setCreatedAt(NOW);
        product.setUpdatedAt(NOW);
        return product;
    }

    public static ProductImage image(Product product, int position, boolean primary) {
        ProductImage image = ProductImage.builder()
                .product(product)
                .objectKey("products/" + product.getId() + "/" + UUID.randomUUID() + ".jpg")
                .contentType("image/jpeg")
                .sizeBytes(64)
                .altText("Imagem " + position)
                .position(position)
                .primaryImage(primary)
                .build();
        image.setId(UUID.randomUUID());
        return image;
    }
}
