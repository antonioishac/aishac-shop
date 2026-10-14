package com.aishac.sales.catalog.service.impl;

import static com.aishac.sales.catalog.CatalogFixtures.PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aishac.sales.catalog.CatalogFixtures;
import com.aishac.sales.catalog.dto.StockMovementRequest;
import com.aishac.sales.catalog.dto.StockMovementResponse;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.StockMovementType;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.ProductRepository;
import com.aishac.sales.catalog.repository.StockMovementRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Pageable;

/** Movimentações de estoque (RN-13 a RN-15), sem Spring nem banco. */
class StockServiceImplTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StockMovementRepository movementRepository = mock(StockMovementRepository.class);
    private final StockServiceImpl service =
            new StockServiceImpl(productRepository, movementRepository, Mappers.getMapper(CatalogMapper.class));

    private Product stored(int stock) {
        Product product = CatalogFixtures.product(CatalogFixtures.category(true), stock);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(movementRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        return product;
    }

    // CA-12
    @Test
    void shouldRecordEntryWithResultingStock() {
        // Given
        stored(10);
        given(productRepository.increaseStock(PRODUCT_ID, 5)).willReturn(Optional.of(15));

        // When
        StockMovementResponse response =
                service.register(PRODUCT_ID, new StockMovementRequest(StockMovementType.ENTRY, 5, "Reposição"));

        // Then
        assertThat(response.resultingStock()).isEqualTo(15);
        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.type()).isEqualTo(StockMovementType.ENTRY);
    }

    // CA-13, RN-14
    @Test
    void shouldRejectExitWhenAtomicUpdateFindsNoStock() {
        // Given
        stored(3);
        given(productRepository.decreaseStock(PRODUCT_ID, 5)).willReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> service.register(
                        PRODUCT_ID, new StockMovementRequest(StockMovementType.EXIT, 5, "Venda balcão")))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INSUFFICIENT_STOCK);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectMovementOfUnknownProduct() {
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.register(PRODUCT_ID, new StockMovementRequest(StockMovementType.ENTRY, 1, "Entrada")))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_NOT_FOUND);
        assertThatThrownBy(() -> service.history(PRODUCT_ID, Pageable.unpaged()))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_NOT_FOUND);
    }
}
