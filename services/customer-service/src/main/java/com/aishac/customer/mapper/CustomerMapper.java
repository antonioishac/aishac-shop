package com.aishac.customer.mapper;

import com.aishac.customer.dto.AddressRequest;
import com.aishac.customer.dto.AddressResponse;
import com.aishac.customer.dto.CustomerResponse;
import com.aishac.customer.dto.OrderDataResponse;
import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Leitura (entidades → respostas) e cópia dos campos simples do endereço (request → entidade). Regras de negócio
 * (normalização de CPF/e-mail, endereço padrão, status) ficam no service, não aqui.
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "isDefault", source = "defaultAddress")
    AddressResponse toResponse(CustomerAddress address);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "status", source = "customer.status")
    @Mapping(target = "shippingAddress", source = "shippingAddress")
    OrderDataResponse toOrderData(Customer customer, CustomerAddress shippingAddress);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "defaultAddress", ignore = true)
    @Mapping(target = "zipCode", ignore = true) // normalizado no service
    void copyAddress(AddressRequest request, @MappingTarget CustomerAddress address);
}
