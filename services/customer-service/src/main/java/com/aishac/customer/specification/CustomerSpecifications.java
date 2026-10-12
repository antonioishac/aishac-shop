package com.aishac.customer.specification;

import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Filtros dinâmicos da listagem de clientes. Evita a explosão de métodos {@code findByXAndY} e usa os índices das
 * migrations: trigramas para o nome, UNIQUE para CPF e e-mail, {@code status + created_at} para o status.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    /**
     * @param name   parte do nome, sem diferenciar maiúsculas
     * @param email  e-mail exato, já normalizado (minúsculas)
     * @param cpf    CPF exato, já normalizado (só dígitos)
     * @param status status do cliente
     */
    public static Specification<Customer> withFilters(String name, String email, String cpf, CustomerStatus status) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (StringUtils.hasText(name)) {
                String pattern = "%" + escapeLike(name.strip().toLowerCase(Locale.ROOT)) + "%";
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("fullName")), pattern, '\\'));
            }
            if (email != null) {
                predicate = cb.and(predicate, cb.equal(root.get("email"), email));
            }
            if (cpf != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cpf"), cpf));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
