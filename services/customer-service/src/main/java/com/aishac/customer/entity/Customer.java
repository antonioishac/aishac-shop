package com.aishac.customer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cliente da loja. Os endereços são gerenciados sempre a partir dele (cascade), sem repositório próprio. O
 * {@code version} é o lock otimista do cadastro inteiro, inclusive dos endereços.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Só os 11 dígitos; imutável depois do cadastro (RN-03), por isso sem setter. */
    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cpf", nullable = false, length = 11, updatable = false)
    private String cpf;

    /** Sempre em minúsculas (RN-04). */
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone", length = 11)
    private String phone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    /** Controlado só pelo Hibernate (RN-09); a versão enviada pelo cliente é comparada no service. */
    @Setter(AccessLevel.NONE)
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder.Default
    @OrderBy("createdAt ASC, id ASC")
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerAddress> addresses = new ArrayList<>();
}
