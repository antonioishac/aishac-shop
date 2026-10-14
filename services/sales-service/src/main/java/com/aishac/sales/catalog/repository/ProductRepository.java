package com.aishac.sales.catalog.repository;

import com.aishac.sales.catalog.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /** Listagem com a categoria na mesma consulta (sem N+1 para montar {@code category} e {@code sellable}). */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(UUID id);

    boolean existsBySku(String sku);

    /**
     * Saída atômica (RN-14): só baixa se houver estoque suficiente no momento da gravação. Devolve o novo saldo, ou
     * vazio se o estoque não bastar (nenhuma linha alterada).
     */
    @Query(
            value = "UPDATE products SET stock_quantity = stock_quantity - :quantity"
                    + " WHERE id = :id AND stock_quantity >= :quantity RETURNING stock_quantity",
            nativeQuery = true)
    Optional<Integer> decreaseStock(@Param("id") UUID id, @Param("quantity") int quantity);

    /** Entrada atômica: devolve o novo saldo, ou vazio se o produto não existir. */
    @Query(
            value = "UPDATE products SET stock_quantity = stock_quantity + :quantity WHERE id = :id"
                    + " RETURNING stock_quantity",
            nativeQuery = true)
    Optional<Integer> increaseStock(@Param("id") UUID id, @Param("quantity") int quantity);

    /**
     * Trava a linha do produto para alterar imagens (limite, posição e principal), sem incrementar a {@code version}:
     * operações de imagem no mesmo produto ficam sequenciais e não invalidam a edição do cadastro.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForImageChange(@Param("id") UUID id);
}
