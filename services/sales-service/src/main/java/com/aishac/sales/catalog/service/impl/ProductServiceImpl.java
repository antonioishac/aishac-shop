package com.aishac.sales.catalog.service.impl;

import com.aishac.sales.catalog.dto.CreateProductRequest;
import com.aishac.sales.catalog.dto.ProductImageResponse;
import com.aishac.sales.catalog.dto.ProductResponse;
import com.aishac.sales.catalog.dto.UpdateProductRequest;
import com.aishac.sales.catalog.entity.Category;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.StockMovement;
import com.aishac.sales.catalog.entity.StockMovementType;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.CategoryRepository;
import com.aishac.sales.catalog.repository.ProductRepository;
import com.aishac.sales.catalog.repository.StockMovementRepository;
import com.aishac.sales.catalog.service.CatalogRules;
import com.aishac.sales.catalog.service.ProductImageService;
import com.aishac.sales.catalog.service.ProductService;
import com.aishac.sales.catalog.specification.CatalogSpecifications;
import com.aishac.sales.exception.BusinessException;
import com.aishac.sales.exception.CommonErrorCode;
import com.aishac.sales.exception.ConstraintViolations;
import com.aishac.sales.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ProductServiceImpl implements ProductService {

    /** Corrida entre cadastros simultâneos: a UNIQUE do banco é a garantia (RN-06, CA-07). */
    private static final String UNIQUE_SKU = "uk_products_sku";

    static final String INITIAL_STOCK_REASON = "Estoque inicial";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductImageService productImageService;
    private final CatalogMapper catalogMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(
            UUID categoryId,
            String name,
            String minPrice,
            String maxPrice,
            Boolean active,
            Boolean sellable,
            Pageable pageable) {
        var specification = CatalogSpecifications.products(
                categoryId,
                name,
                CatalogRules.parsePrice(minPrice),
                CatalogRules.parsePrice(maxPrice),
                active,
                sellable);
        Page<Product> page = productRepository.findAll(specification, pageable);
        // Uma consulta para as imagens principais da página inteira (sem N+1).
        Map<UUID, ProductImageResponse> images = productImageService.findPrimaryImages(
                page.getContent().stream().map(Product::getId).toList());
        return page.map(product -> catalogMapper.toResponse(product, images.get(product.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(findProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = CatalogRules.normalizeSku(request.sku());
        if (productRepository.existsBySku(sku)) {
            throw new BusinessException(CatalogErrorCode.SKU_ALREADY_EXISTS);
        }
        Category category = findActiveCategory(request.categoryId());
        Product product = saveAndFlush(Product.builder()
                .sku(sku)
                .name(request.name())
                .description(request.description())
                .price(CatalogRules.parsePrice(request.price()))
                .category(category)
                .stockQuantity(request.initialStock())
                .build());
        if (request.initialStock() > 0) {
            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .type(StockMovementType.ENTRY)
                    .quantity(request.initialStock())
                    .reason(INITIAL_STOCK_REASON)
                    .resultingStock(request.initialStock())
                    .build());
        }
        return catalogMapper.toResponse(product, null);
    }

    /** RN-10: o estoque não muda aqui (coluna fora do UPDATE da entidade). */
    @Override
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = findProduct(id);
        if (!product.getVersion().equals(request.version())) {
            throw new BusinessException(CommonErrorCode.CONCURRENT_MODIFICATION);
        }
        if (!product.getCategory().getId().equals(request.categoryId())) {
            product.setCategory(findActiveCategory(request.categoryId()));
        }
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(CatalogRules.parsePrice(request.price()));
        return toResponse(saveAndFlush(product));
    }

    @Override
    @Transactional
    public ProductResponse deactivate(UUID id) {
        return changeActive(id, false);
    }

    @Override
    @Transactional
    public ProductResponse activate(UUID id) {
        return changeActive(id, true);
    }

    /** Idempotente: sem mudança, o Hibernate não grava e a {@code version} fica igual. */
    private ProductResponse changeActive(UUID id, boolean active) {
        Product product = findProduct(id);
        product.setActive(active);
        return toResponse(saveAndFlush(product));
    }

    private ProductResponse toResponse(Product product) {
        Map<UUID, ProductImageResponse> images = productImageService.findPrimaryImages(List.of(product.getId()));
        return catalogMapper.toResponse(product, images.get(product.getId()));
    }

    private Product findProduct(UUID id) {
        return productRepository
                .findWithCategoryById(id)
                .orElseThrow(() -> new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, id));
    }

    private Category findActiveCategory(UUID categoryId) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new NotFoundException(CatalogErrorCode.CATEGORY_NOT_FOUND, categoryId));
        CatalogRules.ensureActive(category);
        return category;
    }

    /** Grava já (para devolver version e datas atualizadas) e traduz a corrida de SKU duplicado. */
    private Product saveAndFlush(Product product) {
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.constraintName(e)
                    .filter(UNIQUE_SKU::equals)
                    .isPresent()) {
                throw new BusinessException(CatalogErrorCode.SKU_ALREADY_EXISTS);
            }
            throw e;
        }
    }
}
