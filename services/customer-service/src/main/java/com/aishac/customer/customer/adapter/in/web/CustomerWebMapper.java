package com.aishac.customer.customer.adapter.in.web;

import com.aishac.customer.customer.adapter.in.web.dto.AddressRequest;
import com.aishac.customer.customer.adapter.in.web.dto.AddressResponse;
import com.aishac.customer.customer.adapter.in.web.dto.CustomerPageResponse;
import com.aishac.customer.customer.adapter.in.web.dto.CustomerResponse;
import com.aishac.customer.customer.adapter.in.web.dto.OrderDataResponse;
import com.aishac.customer.customer.application.port.out.CustomerView;
import com.aishac.customer.customer.application.port.out.OrderDataView;
import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.AddressData;
import com.aishac.customer.customer.domain.BrazilianState;
import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.customer.domain.Phone;
import com.aishac.customer.shared.application.PageResult;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;

/** Domínio e modelos de leitura → DTOs da API (e request de endereço → dados de domínio). */
@Mapper
interface CustomerWebMapper {

    CustomerResponse toResponse(CustomerView view);

    CustomerResponse toResponse(Customer customer);

    default String map(Cpf cpf) {
        return cpf.value();
    }

    default String map(Email email) {
        return email.value();
    }

    default @Nullable String map(@Nullable Phone phone) {
        return phone == null ? null : phone.value();
    }

    default AddressResponse toResponse(Address address) {
        AddressData data = address.getData();
        return new AddressResponse(
                address.getId(),
                data.label(),
                data.zipCode(),
                data.street(),
                data.number(),
                data.complement(),
                data.district(),
                data.city(),
                data.state().name(),
                Address.COUNTRY,
                address.isDefault());
    }

    default AddressData toData(AddressRequest request) {
        return new AddressData(
                request.label(),
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                BrazilianState.of(request.state()));
    }

    default OrderDataResponse toResponse(OrderDataView view) {
        Address address = view.shippingAddress();
        return new OrderDataResponse(
                view.customerId(),
                view.fullName(),
                view.email(),
                view.status(),
                address == null ? null : toResponse(address));
    }

    default CustomerPageResponse toResponse(PageResult<CustomerView> page) {
        return new CustomerPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                new CustomerPageResponse.PageMetadata(
                        page.number(), page.size(), page.totalElements(), page.totalPages()));
    }
}
