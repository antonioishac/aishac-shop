package com.aishac.customer.customer.adapter.out.persistence;

import com.aishac.customer.customer.application.port.out.CustomerView;
import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.AddressData;
import com.aishac.customer.customer.domain.BrazilianState;
import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.customer.domain.Phone;
import org.mapstruct.Mapper;

/**
 * Entidade → modelo de leitura (MapStruct, campo a campo) e entidade → agregado (reconstituição explícita pelas
 * fábricas {@code restore}, que o MapStruct não alcança).
 */
@Mapper
interface CustomerPersistenceMapper {

    CustomerView toView(CustomerJpaEntity entity);

    default Customer toDomain(CustomerJpaEntity entity) {
        return Customer.restore(
                entity.getId(),
                entity.getFullName(),
                new Cpf(entity.getCpf()),
                new Email(entity.getEmail()),
                entity.getPhone() == null ? null : new Phone(entity.getPhone()),
                entity.getStatus(),
                entity.getBlockReason(),
                entity.getBlockedAt(),
                entity.getDeactivatedAt(),
                entity.getAddresses().stream().map(this::toDomain).toList(),
                entity.getVersion() == null ? 0 : entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    default Address toDomain(CustomerAddressJpaEntity entity) {
        return Address.restore(
                entity.getId(),
                new AddressData(
                        entity.getLabel(),
                        entity.getZipCode(),
                        entity.getStreet(),
                        entity.getNumber(),
                        entity.getComplement(),
                        entity.getDistrict(),
                        entity.getCity(),
                        BrazilianState.valueOf(entity.getState())),
                entity.isDefault(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
