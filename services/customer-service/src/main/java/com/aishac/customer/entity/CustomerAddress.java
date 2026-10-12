package com.aishac.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Endereço de entrega de um {@link Customer} (1:N). Gerenciado sempre a partir do cliente, por cascade. O banco
 * garante no máximo um padrão por cliente (exclusion constraint adiável, verificada no commit).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "customer_addresses")
public class CustomerAddress extends BaseEntity {

    public static final String BRAZIL = "BR";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Column(name = "label", nullable = false, length = 40)
    private String label;

    /** Só os 8 dígitos. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;

    @Column(name = "street", nullable = false, length = 150)
    private String street;

    @Column(name = "number", nullable = false, length = 10)
    private String number;

    @Column(name = "complement", length = 60)
    private String complement;

    @Column(name = "district", nullable = false, length = 80)
    private String district;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country", nullable = false, length = 2)
    private String country = BRAZIL;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;
}
