package com.aishac.sales.catalog.specification;

import com.aishac.sales.catalog.entity.Category;
import com.aishac.sales.catalog.entity.Product;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Filtros dinâmicos das listagens do catálogo. Usam os índices das migrations: trigramas para o nome do produto,
 * {@code price} para a faixa de preço, FK indexada para a categoria.
 */
public final class CatalogSpecifications {

    private CatalogSpecifications() {}

    public static Specification<Category> categories(String name, Boolean active) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (StringUtils.hasText(name)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), contains(name), '\\'));
            }
            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }
            return predicate;
        };
    }

    /**
     * @param sellable produto e categoria ativos (RN-12); {@code false} traz os não vendáveis
     */
    public static Specification<Product> products(
            UUID categoryId, String name, BigDecimal minPrice, BigDecimal maxPrice, Boolean active, Boolean sellable) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(name)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), contains(name), '\\'));
            }
            if (minPrice != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }
            if (sellable != null) {
                Predicate isSellable = cb.and(
                        cb.isTrue(root.get("active")),
                        cb.isTrue(root.get("category").get("active")));
                predicate = cb.and(predicate, sellable ? isSellable : cb.not(isSellable));
            }
            return predicate;
        };
    }

    private static String contains(String text) {
        String escaped = text.strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
