package com.aishac.sales.catalog.mapper;

import com.aishac.sales.catalog.dto.CategoryResponse;
import com.aishac.sales.catalog.dto.CategorySummary;
import com.aishac.sales.catalog.dto.ProductImageResponse;
import com.aishac.sales.catalog.dto.ProductResponse;
import com.aishac.sales.catalog.dto.StockMovementResponse;
import com.aishac.sales.catalog.entity.Category;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.ProductImage;
import com.aishac.sales.catalog.entity.StockMovement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Leitura do catálogo (entidades → respostas). A imagem principal e a URL pré-assinada chegam prontas do service; aqui
 * só se monta a resposta.
 */
@Mapper(componentModel = "spring")
public interface CatalogMapper {

    CategoryResponse toResponse(Category category);

    CategorySummary toSummary(Category category);

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "sellable", expression = "java(product.isActive() && product.getCategory().isActive())")
    @Mapping(target = "primaryImage", source = "primaryImage")
    ProductResponse toResponse(Product product, ProductImageResponse primaryImage);

    @Mapping(target = "productId", source = "product.id")
    StockMovementResponse toResponse(StockMovement movement);

    @Mapping(target = "id", source = "image.id")
    @Mapping(target = "primary", source = "image.primaryImage")
    ProductImageResponse toResponse(ProductImage image, URI url, Instant urlExpiresAt);

    /** Dinheiro na API: string decimal com 2 casas (ADR-014). */
    default String money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }
}
