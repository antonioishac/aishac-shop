package com.aishac.customer.customer.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** Endereço de entrega. Entidade interna do agregado {@link Customer}: só muda através dele. */
@Getter
public class Address {

    public static final String COUNTRY = "BR";

    private final UUID id;
    private AddressData data;
    private boolean isDefault;
    private final Instant createdAt;
    private Instant updatedAt;

    private Address(UUID id, AddressData data, boolean isDefault, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.data = data;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static Address create(UUID id, AddressData data, boolean isDefault, Instant now) {
        return new Address(id, data, isDefault, now, now);
    }

    /** Reconstitui um endereço já persistido, sem aplicar regras de criação. */
    public static Address restore(UUID id, AddressData data, boolean isDefault, Instant createdAt, Instant updatedAt) {
        return new Address(id, data, isDefault, createdAt, updatedAt);
    }

    void update(AddressData newData, Instant now) {
        this.data = newData;
        this.updatedAt = now;
    }

    void setDefault(boolean value, Instant now) {
        if (this.isDefault != value) {
            this.isDefault = value;
            this.updatedAt = now;
        }
    }
}
