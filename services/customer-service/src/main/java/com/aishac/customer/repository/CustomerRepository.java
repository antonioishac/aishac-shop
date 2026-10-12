package com.aishac.customer.repository;

import com.aishac.customer.entity.Customer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    /** Cliente com os endereços numa única consulta (evita N+1 no order-data). */
    @EntityGraph(attributePaths = "addresses")
    Optional<Customer> findWithAddressesById(UUID id);

    /**
     * Carrega o cliente para alterar endereços: trava a linha até o fim da transação e incrementa a {@code version}
     * na hora. Operações concorrentes no mesmo cliente passam a ser sequenciais, o que garante o limite de 10
     * endereços e o endereço padrão único (RN-12, RN-14), e qualquer leitura anterior fica com versão desatualizada.
     */
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findForAddressChange(@Param("id") UUID id);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
