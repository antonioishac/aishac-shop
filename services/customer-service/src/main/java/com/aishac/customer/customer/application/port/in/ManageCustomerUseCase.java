package com.aishac.customer.customer.application.port.in;

import com.aishac.customer.customer.application.port.in.CustomerCommands.BlockCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.RegisterCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateCustomer;
import com.aishac.customer.customer.domain.Customer;
import java.util.UUID;

/** Cadastro, dados e status do cliente (CA-01 a CA-14). */
public interface ManageCustomerUseCase {

    Customer register(RegisterCustomer command);

    Customer update(UpdateCustomer command);

    Customer block(BlockCustomer command);

    Customer unblock(UUID customerId);

    Customer deactivate(UUID customerId);
}
