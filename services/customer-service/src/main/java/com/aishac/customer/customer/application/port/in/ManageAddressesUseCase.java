package com.aishac.customer.customer.application.port.in;

import com.aishac.customer.customer.application.port.in.CustomerCommands.AddAddress;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateAddress;
import com.aishac.customer.customer.domain.Address;
import java.util.List;
import java.util.UUID;

/** Endereços de entrega do cliente (CA-15 a CA-19). */
public interface ManageAddressesUseCase {

    Address add(AddAddress command);

    Address update(UpdateAddress command);

    void remove(UUID customerId, UUID addressId);

    void makeDefault(UUID customerId, UUID addressId);

    List<Address> list(UUID customerId);
}
