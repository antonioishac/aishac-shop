package com.aishac.sales.catalog.repository;

import com.aishac.sales.catalog.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByPositionAsc(UUID productId);

    Optional<ProductImage> findByIdAndProductId(UUID id, UUID productId);

    long countByProductId(UUID productId);

    /** Imagens principais de uma página de produtos numa só consulta (sem N+1 na listagem). */
    List<ProductImage> findByProductIdInAndPrimaryImageTrue(Collection<UUID> productIds);
}
