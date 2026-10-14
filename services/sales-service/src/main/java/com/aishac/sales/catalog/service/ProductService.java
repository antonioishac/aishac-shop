package com.aishac.sales.catalog.service;

import com.aishac.sales.catalog.dto.CreateProductRequest;
import com.aishac.sales.catalog.dto.ProductResponse;
import com.aishac.sales.catalog.dto.UpdateProductRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /**
     * Filtros combináveis (RN-11). Preços como string decimal já validada; {@code sellable} = produto e categoria
     * ativos (RN-12).
     */
    Page<ProductResponse> list(
            UUID categoryId,
            String name,
            String minPrice,
            String maxPrice,
            Boolean active,
            Boolean sellable,
            Pageable pageable);

    ProductResponse findById(UUID id);

    /** Nasce ativo; o estoque inicial maior que zero vira uma entrada no histórico (RN-09, CA-05). */
    ProductResponse create(CreateProductRequest request);

    ProductResponse update(UUID id, UpdateProductRequest request);

    ProductResponse deactivate(UUID id);

    ProductResponse activate(UUID id);
}
