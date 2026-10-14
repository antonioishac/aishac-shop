package com.aishac.sales.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aishac.sales.catalog.CatalogFixtures;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogRulesTest {

    @Test
    void shouldStoreSkuInUppercase() {
        assertThat(CatalogRules.normalizeSku(" tenis-run-42 ")).isEqualTo("TENIS-RUN-42");
    }

    @Test
    void shouldParsePriceWithTwoDecimalsAndKeepNull() {
        assertThat(CatalogRules.parsePrice("129.9")).isEqualTo(new BigDecimal("129.90"));
        assertThat(CatalogRules.parsePrice("10")).hasToString("10.00");
        assertThat(CatalogRules.parsePrice(null)).isNull();
    }

    @Test
    void shouldRejectInactiveCategory() {
        CatalogRules.ensureActive(CatalogFixtures.category(true));

        assertThatThrownBy(() -> CatalogRules.ensureActive(CatalogFixtures.category(false)))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_INACTIVE);
    }

    @Test
    void shouldGenerateNewObjectKeyForEveryFile() {
        UUID productId = CatalogFixtures.PRODUCT_ID;

        String first = CatalogRules.objectKey(productId, ImageType.PNG);
        String second = CatalogRules.objectKey(productId, ImageType.PNG);

        assertThat(first).startsWith("products/" + productId + "/").endsWith(".png");
        assertThat(first).isNotEqualTo(second);
    }
}
