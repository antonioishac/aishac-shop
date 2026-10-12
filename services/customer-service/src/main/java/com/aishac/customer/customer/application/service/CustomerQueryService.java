package com.aishac.customer.customer.application.service;

import com.aishac.customer.customer.application.port.in.CustomerQueries;
import com.aishac.customer.customer.application.port.out.CustomerFilter;
import com.aishac.customer.customer.application.port.out.CustomerPorts.CustomerQueryPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.LoadCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerView;
import com.aishac.customer.customer.application.port.out.OrderDataView;
import com.aishac.customer.customer.domain.Address;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerException;
import com.aishac.customer.shared.application.PageQuery;
import com.aishac.customer.shared.application.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class CustomerQueryService implements CustomerQueries {

    private final CustomerQueryPort queries;
    private final LoadCustomerPort loadCustomer;

    @Override
    public CustomerView get(UUID customerId) {
        return queries.find(customerId).orElseThrow(() -> CustomerException.customerNotFound(customerId));
    }

    @Override
    public PageResult<CustomerView> search(CustomerFilter filter, PageQuery page) {
        return queries.search(filter, page);
    }

    /** Status real e endereço escolhido (ou o padrão, ou nenhum); quem recusa o pedido é o sales-service. */
    @Override
    public OrderDataView orderData(UUID customerId, @Nullable UUID addressId) {
        Customer customer =
                loadCustomer.load(customerId).orElseThrow(() -> CustomerException.customerNotFound(customerId));
        Address shippingAddress = addressId != null
                ? customer.address(addressId)
                : customer.defaultAddress().orElse(null);
        return new OrderDataView(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail().value(),
                customer.getStatus(),
                shippingAddress);
    }
}
