package com.aishac.customer.customer.application.port.in;

import com.aishac.customer.customer.application.port.out.CustomerFilter;
import com.aishac.customer.customer.application.port.out.CustomerView;
import com.aishac.customer.customer.application.port.out.OrderDataView;
import com.aishac.customer.shared.application.PageQuery;
import com.aishac.customer.shared.application.PageResult;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Consultas do contexto Clientes (CA-06 a CA-08, CA-20, CA-21). */
public interface CustomerQueries {

    CustomerView get(UUID customerId);

    PageResult<CustomerView> search(CustomerFilter filter, PageQuery page);

    OrderDataView orderData(UUID customerId, @Nullable UUID addressId);
}
