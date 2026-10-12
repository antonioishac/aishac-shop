package com.aishac.customer.customer.adapter.out.persistence;

import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.AddressData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "customer_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CustomerAddressJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private CustomerJpaEntity customer;

    @Column(nullable = false, length = 40)
    private String label;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String zipCode;

    @Column(nullable = false, length = 150)
    private String street;

    @Column(nullable = false, length = 10)
    private String number;

    @Column(length = 60)
    private @Nullable String complement;

    @Column(nullable = false, length = 80)
    private String district;

    @Column(nullable = false, length = 80)
    private String city;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String state;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    CustomerAddressJpaEntity(CustomerJpaEntity customer, Address address) {
        this.id = address.getId();
        this.customer = customer;
        this.createdAt = address.getCreatedAt();
        apply(address);
    }

    void apply(Address address) {
        AddressData data = address.getData();
        this.label = data.label();
        this.zipCode = data.zipCode();
        this.street = data.street();
        this.number = data.number();
        this.complement = data.complement();
        this.district = data.district();
        this.city = data.city();
        this.state = data.state().name();
        this.country = Address.COUNTRY;
        this.isDefault = address.isDefault();
        this.updatedAt = address.getUpdatedAt();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CustomerAddressJpaEntity that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return CustomerAddressJpaEntity.class.hashCode();
    }
}
