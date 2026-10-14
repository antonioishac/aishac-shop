package com.aishac.sales.catalog.service.impl;

import static com.aishac.sales.catalog.CatalogFixtures.CATEGORY_ID;
import static com.aishac.sales.catalog.CatalogFixtures.PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aishac.sales.catalog.CatalogFixtures;
import com.aishac.sales.catalog.dto.CreateProductRequest;
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
import com.aishac.sales.catalog.service.ProductImageService;
import com.aishac.sales.exception.CommonErrorCode;
import com.aishac.sales.exception.ErrorCode;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/** Regras de produto (RN-05 a RN-12), sem Spring nem banco. */
class ProductServiceImplTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final StockMovementRepository movementRepository = mock(StockMovementRepository.class);
    private final ProductImageService imageService = mock(ProductImageService.class);
    private final ProductServiceImpl service = new ProductServiceImpl(
            productRepository,
            categoryRepository,
            movementRepository,
            imageService,
            Mappers.getMapper(CatalogMapper.class));

    private static CreateProductRequest create(int initialStock) {
        return new CreateProductRequest("tenis-run-42", "Tênis Run", null, "299.9", CATEGORY_ID, initialStock);
    }

    private static UpdateProductRequest update(UUID categoryId, long version) {
        return new UpdateProductRequest("Tênis Run 2", "Nova", "279.90", categoryId, version);
    }

    private static void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).extracting("errorCode").isEqualTo(code);
    }

    private Product stored(Category category) {
        Product product = CatalogFixtures.product(category, 10);
        given(productRepository.findWithCategoryById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(productRepository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(imageService.findPrimaryImages(anyCollection())).willReturn(Map.of());
        return product;
    }

    // CA-05, RN-06, RN-09
    @Test
    void shouldCreateProductWithUppercaseSkuAndInitialEntry() {
        // Given
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(true)));
        given(productRepository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductResponse response = service.create(create(10));

        // Then
        assertThat(response.sku()).isEqualTo("TENIS-RUN-42");
        assertThat(response.price()).isEqualTo("299.90");
        assertThat(response.stockQuantity()).isEqualTo(10);
        assertThat(response.sellable()).isTrue();
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(movement.capture());
        assertThat(movement.getValue().getType()).isEqualTo(StockMovementType.ENTRY);
        assertThat(movement.getValue().getResultingStock()).isEqualTo(10);
    }

    @Test
    void shouldNotRecordMovementForZeroInitialStock() {
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(true)));
        given(productRepository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.create(create(0));

        verify(movementRepository, never()).save(any());
    }

    // CA-07
    @Test
    void shouldRejectExistingSkuAndTranslateUniqueRace() {
        // Given: SKU já cadastrado
        given(productRepository.existsBySku("TENIS-RUN-42")).willReturn(true);

        // Then
        assertError(() -> service.create(create(0)), CatalogErrorCode.SKU_ALREADY_EXISTS);

        // Given: outro cadastro gravou o mesmo SKU entre a checagem e o INSERT
        given(productRepository.existsBySku("TENIS-RUN-42")).willReturn(false);
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(true)));
        given(productRepository.saveAndFlush(any())).willThrow(uniqueViolation("uk_products_sku"));

        // Then
        assertError(() -> service.create(create(0)), CatalogErrorCode.SKU_ALREADY_EXISTS);
    }

    @Test
    void shouldPropagateOtherIntegrityViolations() {
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(true)));
        given(productRepository.saveAndFlush(any())).willThrow(uniqueViolation("ck_products_price_positive"));

        assertThatThrownBy(() -> service.create(create(0))).isInstanceOf(DataIntegrityViolationException.class);
    }

    // CA-08, RN-08
    @Test
    void shouldRejectInactiveOrUnknownCategory() {
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(false)));
        assertError(() -> service.create(create(0)), CatalogErrorCode.CATEGORY_INACTIVE);

        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());
        assertError(() -> service.create(create(0)), CatalogErrorCode.CATEGORY_NOT_FOUND);
    }

    // CA-09, RN-10
    @Test
    void shouldUpdateOnlyWithCurrentVersionAndKeepStock() {
        // Given
        Product product = stored(CatalogFixtures.category(true));

        // When
        ProductResponse response = service.update(PRODUCT_ID, update(CATEGORY_ID, 0));

        // Then
        assertThat(response.name()).isEqualTo("Tênis Run 2");
        assertThat(response.price()).isEqualTo("279.90");
        assertThat(response.stockQuantity()).isEqualTo(10);
        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertError(() -> service.update(PRODUCT_ID, update(CATEGORY_ID, 7)), CommonErrorCode.CONCURRENT_MODIFICATION);
    }

    @Test
    void shouldKeepProductInItsInactiveCategoryButNotMoveToAnother() {
        // Given: a categoria atual foi desativada depois do cadastro
        stored(CatalogFixtures.category(false));
        UUID other = UUID.randomUUID();
        given(categoryRepository.findById(other)).willReturn(Optional.of(CatalogFixtures.category(other, false)));

        // Then
        assertThat(service.update(PRODUCT_ID, update(CATEGORY_ID, 0)).sellable())
                .isFalse();
        assertError(() -> service.update(PRODUCT_ID, update(other, 0)), CatalogErrorCode.CATEGORY_INACTIVE);
    }

    // CA-11
    @Test
    void shouldDeactivateAndActivate() {
        stored(CatalogFixtures.category(true));

        assertThat(service.deactivate(PRODUCT_ID).active()).isFalse();
        assertThat(service.activate(PRODUCT_ID).sellable()).isTrue();
    }

    @Test
    void shouldAnswerNotFoundForUnknownProduct() {
        given(productRepository.findWithCategoryById(PRODUCT_ID)).willReturn(Optional.empty());

        assertError(() -> service.findById(PRODUCT_ID), CatalogErrorCode.PRODUCT_NOT_FOUND);
    }

    private static DataIntegrityViolationException uniqueViolation(String constraint) {
        return new DataIntegrityViolationException(
                "duplicate", new ConstraintViolationException("duplicate", new SQLException(), constraint));
    }
}
