package com.aishac.customer.customer.application.service;

import com.aishac.customer.customer.application.port.in.CustomerCommands.BlockCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.RegisterCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateCustomer;
import com.aishac.customer.customer.application.port.in.ManageCustomerUseCase;
import com.aishac.customer.customer.application.port.out.CustomerPorts.LoadCustomerPort;
import com.aishac.customer.customer.application.port.out.CustomerPorts.SaveCustomerPort;
import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerException;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.customer.domain.Phone;
import com.aishac.customer.shared.application.IdGenerator;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class CustomerService implements ManageCustomerUseCase {

    private final LoadCustomerPort loadCustomer;
    private final SaveCustomerPort saveCustomer;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public Customer register(RegisterCustomer command) {
        Cpf cpf = Cpf.of(command.cpf());
        Email email = Email.of(command.email());
        // Checagem prévia só para a mensagem; a garantia em corrida é a UNIQUE (traduzida no adapter).
        if (loadCustomer.existsByCpf(cpf)) {
            throw CustomerException.cpfAlreadyExists();
        }
        if (loadCustomer.existsByEmail(email)) {
            throw CustomerException.emailAlreadyExists();
        }
        Customer customer = Customer.register(
                idGenerator.newId(),
                command.fullName(),
                cpf,
                email,
                Phone.ofNullable(command.phone()),
                clock.instant());
        return saveCustomer.save(customer);
    }

    @Override
    public Customer update(UpdateCustomer command) {
        Customer customer = load(command.customerId());
        if (customer.getVersion() != command.expectedVersion()) {
            throw CustomerException.concurrentModification();
        }
        Email email = Email.of(command.email());
        if (loadCustomer.existsByEmailOfAnotherCustomer(email, customer.getId())) {
            throw CustomerException.emailAlreadyExists();
        }
        customer.updateDetails(command.fullName(), email, Phone.ofNullable(command.phone()), clock.instant());
        return saveCustomer.save(customer);
    }

    @Override
    public Customer block(BlockCustomer command) {
        return change(command.customerId(), customer -> customer.block(command.reason(), clock.instant()));
    }

    @Override
    public Customer unblock(UUID customerId) {
        return change(customerId, customer -> customer.unblock(clock.instant()));
    }

    @Override
    public Customer deactivate(UUID customerId) {
        return change(customerId, customer -> customer.deactivate(clock.instant()));
    }

    private Customer change(UUID customerId, Consumer<Customer> action) {
        Customer customer = load(customerId);
        action.accept(customer);
        return saveCustomer.save(customer);
    }

    private Customer load(UUID customerId) {
        return loadCustomer.load(customerId).orElseThrow(() -> CustomerException.customerNotFound(customerId));
    }
}
