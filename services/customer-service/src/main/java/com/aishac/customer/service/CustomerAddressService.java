package com.aishac.customer.service;

import com.aishac.customer.dto.AddressRequest;
import com.aishac.customer.dto.AddressResponse;
import java.util.List;
import java.util.UUID;

public interface CustomerAddressService {

    /** Endereços com o padrão primeiro e os demais na ordem de cadastro. */
    List<AddressResponse> list(UUID customerId);

    AddressResponse add(UUID customerId, AddressRequest request);

    AddressResponse update(UUID customerId, UUID addressId, AddressRequest request);

    void remove(UUID customerId, UUID addressId);

    void setDefault(UUID customerId, UUID addressId);
}
