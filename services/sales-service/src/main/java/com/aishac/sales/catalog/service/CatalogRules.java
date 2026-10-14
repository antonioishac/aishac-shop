package com.aishac.sales.catalog.service;

import com.aishac.sales.catalog.entity.Category;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

/** Regras pequenas do catálogo, compartilhadas pelos services (sem estado, testáveis sem Spring). */
public final class CatalogRules {

    private CatalogRules() {}

    /** RN-06: o SKU é guardado em maiúsculas. */
    public static String normalizeSku(String sku) {
        return sku.strip().toUpperCase(Locale.ROOT);
    }

    /** RN-07: string decimal já validada no DTO → {@link BigDecimal} com escala 2. Nulo continua nulo (filtros). */
    public static BigDecimal parsePrice(String price) {
        return price == null ? null : new BigDecimal(price.strip()).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** RN-08: produto só é cadastrado ou movido para categoria ativa. */
    public static void ensureActive(Category category) {
        if (!category.isActive()) {
            throw new BusinessException(CatalogErrorCode.CATEGORY_INACTIVE);
        }
    }

    /** RN-18/RN-19: {@code products/{productId}/{uuid}.{ext}}; cada arquivo gravado ganha uma chave nova. */
    public static String objectKey(UUID productId, ImageType type) {
        return "products/" + productId + "/" + UUID.randomUUID() + "." + type.extension();
    }
}
