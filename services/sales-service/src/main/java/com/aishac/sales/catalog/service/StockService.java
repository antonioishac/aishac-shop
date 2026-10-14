package com.aishac.sales.catalog.service;

import com.aishac.sales.catalog.dto.StockMovementRequest;
import com.aishac.sales.catalog.dto.StockMovementResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockService {

    /** Entrada ou saída; a saída é atômica e nunca deixa o estoque negativo (RN-13, RN-14). */
    StockMovementResponse register(UUID productId, StockMovementRequest request);

    /** Histórico do produto, da movimentação mais recente para a mais antiga (RN-15). */
    Page<StockMovementResponse> history(UUID productId, Pageable pageable);
}
