package com.aishac.sales.catalog.service;

import com.aishac.sales.catalog.dto.CategoryResponse;
import com.aishac.sales.catalog.dto.CreateCategoryRequest;
import com.aishac.sales.catalog.dto.UpdateCategoryRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    /** Filtros combináveis: nome contém (sem diferenciar maiúsculas) e situação. */
    Page<CategoryResponse> list(String name, Boolean active, Pageable pageable);

    CategoryResponse findById(UUID id);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(UUID id, UpdateCategoryRequest request);

    /** Idempotente. Os produtos não mudam, mas deixam de ser vendáveis (RN-04). */
    CategoryResponse deactivate(UUID id);

    CategoryResponse activate(UUID id);
}
