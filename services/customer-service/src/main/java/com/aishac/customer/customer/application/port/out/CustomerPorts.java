package com.aishac.customer.customer.application.port.out;

import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.shared.application.PageQuery;
import com.aishac.customer.shared.application.PageResult;
import java.util.Optional;
import java.util.UUID;

/** Ports de saída do contexto Clientes, implementados pelo adapter de persistência. */
public final class CustomerPorts {

    private CustomerPorts() {}

    /** Carrega o agregado completo (com endereços) para comandos. */
    public interface LoadCustomerPort {

        Optional<Customer> load(UUID customerId);

        boolean existsByCpf(Cpf cpf);

        boolean existsByEmailOfAnotherCustomer(Email email, UUID customerId);

        boolean existsByEmail(Email email);
    }

    /**
     * Grava o agregado e devolve o estado persistido (com a nova {@code version}). Falha com
     * CONCURRENT_MODIFICATION se a versão do agregado não for a do banco, e traduz violações de unicidade
     * de CPF e e-mail.
     */
    public interface SaveCustomerPort {

        Customer save(Customer customer);
    }

    /** Leituras sem carregar endereços. */
    public interface CustomerQueryPort {

        Optional<CustomerView> find(UUID customerId);

        PageResult<CustomerView> search(CustomerFilter filter, PageQuery page);
    }
}
