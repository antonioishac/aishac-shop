package com.aishac.customer.customer.application.service;

import com.aishac.customer.customer.application.port.in.CustomerCommands.AddAddress;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateAddress;
import com.aishac.customer.customer.application.port.in.ManageAddressesUseCase;
import com.aishac.customer.customer.application.port.out.CustomerPorts.LoadCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.SaveCustomerPort;
import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerException;
import com.aishac.customer.shared.application.IdGenerator;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endereços passam pelo agregado: toda alteração muda o cliente e incrementa a version dele, o que serializa
 * operações concorrentes (limite de 10, endereço padrão único).
 */
@Service
@Transactional
@RequiredArgsConstructor
class CustomerAddressService implements ManageAddressesUseCase {

    private final LoadCustomerPort loadCustomer;
    private final SaveCustomerPort saveCustomer;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public Address add(AddAddress command) {
        Customer customer = load(command.customerId());
        Address address = customer.addAddress(idGenerator.newId(), command.data(), clock.instant());
        return saveCustomer.save(customer).address(address.getId());
    }

    @Override
    public Address update(UpdateAddress command) {
        Customer customer = load(command.customerId());
        customer.updateAddress(command.addressId(), command.data(), clock.instant());
        return saveCustomer.save(customer).address(command.addressId());
    }

    @Override
    public void remove(UUID customerId, UUID addressId) {
        Customer customer = load(customerId);
        customer.removeAddress(addressId, clock.instant());
        saveCustomer.save(customer);
    }

    @Override
    public void makeDefault(UUID customerId, UUID addressId) {
        Customer customer = load(customerId);
        customer.makeDefault(addressId, clock.instant());
        saveCustomer.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> list(UUID customerId) {
        return load(customerId).getAddresses();
    }

    private Customer load(UUID customerId) {
        return loadCustomer.load(customerId).orElseThrow(() -> CustomerException.customerNotFound(customerId));
    }
}
