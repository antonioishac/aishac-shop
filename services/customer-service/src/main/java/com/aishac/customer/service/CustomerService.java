package com.aishac.customer.service;

import com.aishac.customer.dto.BlockCustomerRequest;
import com.aishac.customer.dto.CustomerRequest;
import com.aishac.customer.dto.CustomerResponse;
import com.aishac.customer.dto.CustomerUpdateRequest;
import com.aishac.customer.dto.OrderDataResponse;
import com.aishac.customer.entity.CustomerStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    /** Filtros combináveis: nome contém; e-mail e CPF exatos (checkout, CA-08); status. */
    Page<CustomerResponse> list(String name, String email, String cpf, CustomerStatus status, Pageable pageable);

    CustomerResponse findById(UUID id);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(UUID id, CustomerUpdateRequest request);

    CustomerResponse block(UUID id, BlockCustomerRequest request);

    CustomerResponse unblock(UUID id);

    CustomerResponse deactivate(UUID id);

    /** Dados para pedido no sales-service: endereço escolhido, o padrão, ou nenhum (RN-16, RN-17). */
    OrderDataResponse findOrderData(UUID customerId, UUID addressId);
}
