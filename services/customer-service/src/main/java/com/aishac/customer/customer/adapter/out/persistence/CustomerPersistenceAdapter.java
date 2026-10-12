package com.aishac.customer.customer.adapter.out.persistence;

import com.aishac.customer.customer.application.port.out.CustomerFilter;
import com.aishac.customer.customer.application.port.out.CustomerPorts.CustomerQueryPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.LoadCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.SaveCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerView;
import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerException;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.shared.application.PageQuery;
import com.aishac.customer.shared.application.PageResult;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CustomerPersistenceAdapter implements LoadCustomerPort, SaveCustomerPort, CustomerQueryPort {

    /** Violações de unicidade traduzidas para o catálogo (corrida entre cadastros simultâneos). */
    private static final Map<String, Supplier<CustomerException>> UNIQUE_VIOLATIONS = Map.of(
            "uk_customers_cpf", CustomerException::cpfAlreadyExists,
            "uk_customers_email", CustomerException::emailAlreadyExists);

    /** Propriedades ordenáveis expostas pela API → propriedades da entidade. */
    private static final Map<String, String> SORTABLE = Map.of("fullName", "fullName", "createdAt", "createdAt");

    private final CustomerJpaRepository repository;
    private final CustomerPersistenceMapper mapper;

    // --- LoadCustomerPort ---------------------------------------------------------------------

    @Override
    public Optional<Customer> load(UUID customerId) {
        return repository.findWithAddressesById(customerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCpf(Cpf cpf) {
        return repository.existsByCpf(cpf.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByEmailOfAnotherCustomer(Email email, UUID customerId) {
        return repository.existsByEmailAndIdNot(email.value(), customerId);
    }

    // --- SaveCustomerPort ---------------------------------------------------------------------

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity =
                repository.findWithAddressesById(customer.getId()).orElseGet(() -> CustomerJpaEntity.newFor(customer));
        Long persistedVersion = entity.getVersion();
        if (persistedVersion != null && persistedVersion != customer.getVersion()) {
            throw CustomerException.concurrentModification();
        }
        entity.apply(customer);
        try {
            return mapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        return UNIQUE_VIOLATIONS.entrySet().stream()
                .filter(violation -> message.contains(violation.getKey()))
                .<RuntimeException>map(violation -> violation.getValue().get())
                .findFirst()
                .orElse(e);
    }

    // --- CustomerQueryPort --------------------------------------------------------------------

    @Override
    public Optional<CustomerView> find(UUID customerId) {
        return repository.findById(customerId).map(mapper::toView);
    }

    @Override
    public PageResult<CustomerView> search(CustomerFilter filter, PageQuery page) {
        String property = SORTABLE.get(page.sortProperty());
        if (property == null) {
            throw new IllegalArgumentException("unsupported sort property: " + page.sortProperty());
        }
        Sort sort = Sort.by(page.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, property)
                .and(Sort.by("id")); // desempate estável entre páginas
        Page<CustomerJpaEntity> result =
                repository.findAll(matching(filter), PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(
                result.getContent().stream().map(mapper::toView).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static Specification<CustomerJpaEntity> matching(CustomerFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.name() != null) {
                // lower(full_name) LIKE ... usa o índice GIN de trigramas (idx_customers_full_name_trgm).
                String pattern = "%" + escapeLike(filter.name().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("fullName")), pattern, '\\'));
            }
            if (filter.email() != null) {
                predicates.add(cb.equal(root.get("email"), filter.email().value()));
            }
            if (filter.cpf() != null) {
                predicates.add(cb.equal(root.get("cpf"), filter.cpf().value()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
