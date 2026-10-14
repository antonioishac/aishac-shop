package com.aishac.sales.catalog.repository;

import com.aishac.sales.catalog.entity.StockMovement;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    /** Histórico do produto; a ordenação vem do {@link Pageable} (mais recente primeiro). */
    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);
}
