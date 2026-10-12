package com.aishac.customer.service.impl;

import com.aishac.customer.dto.AddressRequest;
import com.aishac.customer.dto.AddressResponse;
import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerAddress;
import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CustomerErrorCode;
import com.aishac.customer.exception.NotFoundException;
import com.aishac.customer.mapper.CustomerMapper;
import com.aishac.customer.repository.CustomerRepository;
import com.aishac.customer.service.CustomerAddressService;
import com.aishac.customer.service.CustomerRules;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endereços do cliente (RN-11 a RN-15). Toda alteração carrega o cliente com trava e incremento de versão
 * ({@code findForAddressChange}): operações simultâneas no mesmo cliente são executadas uma de cada vez.
 */
@Service
@RequiredArgsConstructor
class CustomerAddressServiceImpl implements CustomerAddressService {

    /** Padrão primeiro; depois por ordem de cadastro (id UUID v7 desempata no mesmo instante). */
    private static final Comparator<CustomerAddress> DEFAULT_FIRST = Comparator.comparing(
                    (CustomerAddress address) -> !address.isDefaultAddress())
            .thenComparing(CustomerAddress::getCreatedAt)
            .thenComparing(CustomerAddress::getId);

    private static final Comparator<CustomerAddress> OLDEST_FIRST =
            Comparator.comparing(CustomerAddress::getCreatedAt).thenComparing(CustomerAddress::getId);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID customerId) {
        Customer customer = customerRepository
                .findWithAddressesById(customerId)
                .orElseThrow(() -> new NotFoundException(CustomerErrorCode.CUSTOMER_NOT_FOUND, customerId));
        return customer.getAddresses().stream()
                .sorted(DEFAULT_FIRST)
                .map(customerMapper::toResponse)
                .toList();
    }

    /** O primeiro endereço vira padrão (RN-13); no máximo 10 por cliente (RN-12). */
    @Override
    @Transactional
    public AddressResponse add(UUID customerId, AddressRequest request) {
        Customer customer = lockForChange(customerId);
        if (customer.getAddresses().size() >= CustomerRules.MAX_ADDRESSES) {
            throw new BusinessException(CustomerErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }
        CustomerAddress address = new CustomerAddress();
        customerMapper.copyAddress(request, address);
        address.setCustomer(customer);
        address.setDefaultAddress(customer.getAddresses().isEmpty());
        customer.getAddresses().add(address);
        customerRepository.flush();
        return customerMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse update(UUID customerId, UUID addressId, AddressRequest request) {
        Customer customer = lockForChange(customerId);
        CustomerAddress address = findAddress(customer, addressId);
        customerMapper.copyAddress(request, address);
        customerRepository.flush();
        return customerMapper.toResponse(address);
    }

    /** Se o removido era o padrão, o cadastrado mais recentemente vira o padrão (RN-15). */
    @Override
    @Transactional
    public void remove(UUID customerId, UUID addressId) {
        Customer customer = lockForChange(customerId);
        CustomerAddress removed = findAddress(customer, addressId);
        customer.getAddresses().remove(removed);
        if (removed.isDefaultAddress()) {
            customer.getAddresses().stream().max(OLDEST_FIRST).ifPresent(newest -> newest.setDefaultAddress(true));
        }
    }

    /** Desmarca o padrão anterior na mesma transação (RN-14). Idempotente. */
    @Override
    @Transactional
    public void setDefault(UUID customerId, UUID addressId) {
        Customer customer = lockForChange(customerId);
        CustomerAddress target = findAddress(customer, addressId);
        customer.getAddresses().forEach(address -> address.setDefaultAddress(address == target));
    }

    private Customer lockForChange(UUID customerId) {
        Customer customer = customerRepository
                .findForAddressChange(customerId)
                .orElseThrow(() -> new NotFoundException(CustomerErrorCode.CUSTOMER_NOT_FOUND, customerId));
        CustomerRules.ensureNotInactive(customer);
        return customer;
    }

    private static CustomerAddress findAddress(Customer customer, UUID addressId) {
        return customer.getAddresses().stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(CustomerErrorCode.ADDRESS_NOT_FOUND, addressId));
    }
}
