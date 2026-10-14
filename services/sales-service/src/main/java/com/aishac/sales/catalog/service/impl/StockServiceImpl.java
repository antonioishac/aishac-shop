package com.aishac.sales.catalog.service.impl;

import com.aishac.sales.catalog.dto.StockMovementRequest;
import com.aishac.sales.catalog.dto.StockMovementResponse;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.StockMovement;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.ProductRepository;
import com.aishac.sales.catalog.repository.StockMovementRepository;
import com.aishac.sales.catalog.service.StockService;
import com.aishac.sales.exception.BusinessException;
import com.aishac.sales.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CatalogMapper catalogMapper;

    /**
     * O saldo muda num único {@code UPDATE ... RETURNING} (RN-14): a saída só acontece se o estoque bastar no momento
     * da gravação, sem ler-e-depois-gravar. O histórico é gravado na mesma transação (RN-15).
     */
    @Override
    @Transactional
    public StockMovementResponse register(UUID productId, StockMovementRequest request) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId));
        int resultingStock = switch (request.type()) {
            case ENTRY ->
                productRepository
                        .increaseStock(productId, request.quantity())
                        .orElseThrow(() -> new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId));
            case EXIT ->
                productRepository
                        .decreaseStock(productId, request.quantity())
                        .orElseThrow(() -> new BusinessException(CatalogErrorCode.INSUFFICIENT_STOCK));
        };
        StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .type(request.type())
                .quantity(request.quantity())
                .reason(request.reason())
                .resultingStock(resultingStock)
                .build());
        return catalogMapper.toResponse(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> history(UUID productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId);
        }
        return stockMovementRepository.findByProductId(productId, pageable).map(catalogMapper::toResponse);
    }
}
