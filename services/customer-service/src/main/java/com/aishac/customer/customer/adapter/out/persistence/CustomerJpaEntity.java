package com.aishac.customer.customer.adapter.out.persistence;

import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerStatus;
import com.aishac.customer.customer.domain.Phone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CustomerJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 11, updatable = false)
    private String cpf;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 11)
    private @Nullable String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;

    @Column(length = 255)
    private @Nullable String blockReason;

    private @Nullable Instant blockedAt;

    private @Nullable Instant deactivatedAt;

    /** Nulo enquanto novo: o Spring Data usa isso para decidir entre persist e merge. */
    @Version
    private @Nullable Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerAddressJpaEntity> addresses = new ArrayList<>();

    static CustomerJpaEntity newFor(Customer customer) {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.id = customer.getId();
        entity.cpf = customer.getCpf().value();
        entity.createdAt = customer.getCreatedAt();
        return entity;
    }

    /** Copia o estado do agregado, sincronizando os endereços pelo id (inclui, altera e remove). */
    void apply(Customer customer) {
        this.fullName = customer.getFullName();
        this.email = customer.getEmail().value();
        Phone customerPhone = customer.getPhone();
        this.phone = customerPhone == null ? null : customerPhone.value();
        this.status = customer.getStatus();
        this.blockReason = customer.getBlockReason();
        this.blockedAt = customer.getBlockedAt();
        this.deactivatedAt = customer.getDeactivatedAt();
        this.updatedAt = customer.getUpdatedAt();

        Map<UUID, Address> current =
                customer.getAddresses().stream().collect(Collectors.toMap(Address::getId, Function.identity()));
        addresses.removeIf(entity -> !current.containsKey(entity.getId()));
        Map<UUID, CustomerAddressJpaEntity> persisted =
                addresses.stream().collect(Collectors.toMap(CustomerAddressJpaEntity::getId, Function.identity()));
        current.values().forEach(address -> {
            CustomerAddressJpaEntity entity = persisted.get(address.getId());
            if (entity == null) {
                addresses.add(new CustomerAddressJpaEntity(this, address));
            } else {
                entity.apply(address);
            }
        });
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CustomerJpaEntity that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return CustomerJpaEntity.class.hashCode();
    }
}
