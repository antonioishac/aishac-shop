package com.aishac.sales.catalog.service.impl;

import com.aishac.sales.catalog.dto.CategoryResponse;
import com.aishac.sales.catalog.dto.CreateCategoryRequest;
import com.aishac.sales.catalog.dto.UpdateCategoryRequest;
import com.aishac.sales.catalog.entity.Category;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.CategoryRepository;
import com.aishac.sales.catalog.service.CategoryService;
import com.aishac.sales.catalog.specification.CatalogSpecifications;
import com.aishac.sales.exception.BusinessException;
import com.aishac.sales.exception.CommonErrorCode;
import com.aishac.sales.exception.ConstraintViolations;
import com.aishac.sales.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CategoryServiceImpl implements CategoryService {

    /** Corrida entre cadastros simultâneos: o índice único do banco é a garantia (RN-02). */
    private static final String UNIQUE_NAME = "uk_categories_name_lower";

    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(String name, Boolean active, Pageable pageable) {
        return categoryRepository
                .findAll(CatalogSpecifications.categories(name, active), pageable)
                .map(catalogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return catalogMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException(CatalogErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return catalogMapper.toResponse(saveAndFlush(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = findCategory(id);
        if (!category.getVersion().equals(request.version())) {
            throw new BusinessException(CommonErrorCode.CONCURRENT_MODIFICATION);
        }
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new BusinessException(CatalogErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
        }
        category.setName(request.name());
        category.setDescription(request.description());
        return catalogMapper.toResponse(saveAndFlush(category));
    }

    @Override
    @Transactional
    public CategoryResponse deactivate(UUID id) {
        return changeActive(id, false);
    }

    @Override
    @Transactional
    public CategoryResponse activate(UUID id) {
        return changeActive(id, true);
    }

    /** Idempotente: sem mudança, o Hibernate não grava e a {@code version} fica igual. */
    private CategoryResponse changeActive(UUID id, boolean active) {
        Category category = findCategory(id);
        category.setActive(active);
        return catalogMapper.toResponse(saveAndFlush(category));
    }

    private Category findCategory(UUID id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(CatalogErrorCode.CATEGORY_NOT_FOUND, id));
    }

    /** Grava já (para devolver version e datas atualizadas) e traduz a corrida de nome duplicado. */
    private Category saveAndFlush(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.constraintName(e)
                    .filter(UNIQUE_NAME::equals)
                    .isPresent()) {
                throw new BusinessException(CatalogErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
            }
            throw e;
        }
    }
}
