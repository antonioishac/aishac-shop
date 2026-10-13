package com.aishac.customer.service.impl;

import com.aishac.customer.dto.BlockCustomerRequest;
import com.aishac.customer.dto.CustomerRequest;
import com.aishac.customer.dto.CustomerResponse;
import com.aishac.customer.dto.CustomerUpdateRequest;
import com.aishac.customer.dto.OrderDataResponse;
import com.aishac.customer.entity.Customer;
import com.aishac.customer.entity.CustomerAddress;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CommonErrorCode;
import com.aishac.customer.exception.ConstraintViolations;
import com.aishac.customer.exception.CustomerErrorCode;
import com.aishac.customer.exception.NotFoundException;
import com.aishac.customer.mapper.CustomerMapper;
import com.aishac.customer.repository.CustomerRepository;
import com.aishac.customer.service.CpfValidator;
import com.aishac.customer.service.CustomerRules;
import com.aishac.customer.service.CustomerService;
import com.aishac.customer.specification.CustomerSpecifications;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class CustomerServiceImpl implements CustomerService {

    /** Corrida entre cadastros simultâneos: a UNIQUE do banco é a garantia; aqui vira o código do catálogo. */
    private static final Map<String, CustomerErrorCode> UNIQUE_CONSTRAINTS = Map.of(
            "uk_customers_cpf", CustomerErrorCode.CPF_ALREADY_EXISTS,
            "uk_customers_email", CustomerErrorCode.EMAIL_ALREADY_EXISTS);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(
            String name, String email, String cpf, CustomerStatus status, Pageable pageable) {
        var specification = CustomerSpecifications.withFilters(
                name,
                StringUtils.hasText(email) ? CustomerRules.normalizeEmail(email) : null,
                StringUtils.hasText(cpf) ? CpfValidator.normalize(cpf) : null,
                status);
        // A resposta não expõe endereços: a coleção lazy nunca é tocada, sem N+1.
        return customerRepository.findAll(specification, pageable).map(customerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return customerMapper.toResponse(findCustomer(id));
    }

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String cpf = CpfValidator.normalize(request.cpf());
        String email = CustomerRules.normalizeEmail(request.email());
        if (customerRepository.existsByCpf(cpf)) {
            throw new BusinessException(CustomerErrorCode.CPF_ALREADY_EXISTS);
        }
        if (customerRepository.existsByEmail(email)) {
            throw new BusinessException(CustomerErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Customer customer = Customer.builder()
                .fullName(request.fullName())
                .cpf(cpf)
                .email(email)
                .phone(request.phone())
                .status(CustomerStatus.ACTIVE)
                .build();
        return customerMapper.toResponse(saveAndFlush(customer));
    }

    @Override
    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = findCustomer(id);
        CustomerRules.ensureNotInactive(customer);
        if (!customer.getVersion().equals(request.version())) {
            throw new BusinessException(CommonErrorCode.CONCURRENT_MODIFICATION);
        }
        String email = CustomerRules.normalizeEmail(request.email());
        if (customerRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException(CustomerErrorCode.EMAIL_ALREADY_EXISTS);
        }
        customer.setFullName(request.fullName());
        customer.setEmail(email);
        customer.setPhone(request.phone());
        return customerMapper.toResponse(saveAndFlush(customer));
    }

    @Override
    @Transactional
    public CustomerResponse block(UUID id, BlockCustomerRequest request) {
        Customer customer = findCustomer(id);
        CustomerRules.ensureCanTransition(customer, CustomerStatus.BLOCKED);
        customer.setStatus(CustomerStatus.BLOCKED);
        customer.setBlockReason(request.reason());
        customer.setBlockedAt(clock.instant());
        return customerMapper.toResponse(saveAndFlush(customer));
    }

    @Override
    @Transactional
    public CustomerResponse unblock(UUID id) {
        Customer customer = findCustomer(id);
        CustomerRules.ensureCanTransition(customer, CustomerStatus.ACTIVE);
        customer.setStatus(CustomerStatus.ACTIVE);
        clearBlock(customer);
        return customerMapper.toResponse(saveAndFlush(customer));
    }

    /** Encerra o cadastro sem excluir (RN-07, ADR-016). */
    @Override
    @Transactional
    public CustomerResponse deactivate(UUID id) {
        Customer customer = findCustomer(id);
        CustomerRules.ensureCanTransition(customer, CustomerStatus.INACTIVE);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setDeactivatedAt(clock.instant());
        clearBlock(customer);
        return customerMapper.toResponse(saveAndFlush(customer));
    }

    /** Devolve o status real: quem recusa o pedido de cliente não ativo é o sales-service (RN-10). */
    @Override
    @Transactional(readOnly = true)
    public OrderDataResponse findOrderData(UUID customerId, UUID addressId) {
        Customer customer = customerRepository
                .findWithAddressesById(customerId)
                .orElseThrow(() -> new NotFoundException(CustomerErrorCode.CUSTOMER_NOT_FOUND, customerId));
        CustomerAddress shippingAddress = addressId != null
                ? customer.getAddresses().stream()
                        .filter(address -> address.getId().equals(addressId))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException(CustomerErrorCode.ADDRESS_NOT_FOUND, addressId))
                : customer.getAddresses().stream()
                        .filter(CustomerAddress::isDefaultAddress)
                        .findFirst()
                        .orElse(null);
        return customerMapper.toOrderData(customer, shippingAddress);
    }

    private Customer findCustomer(UUID id) {
        return customerRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(CustomerErrorCode.CUSTOMER_NOT_FOUND, id));
    }

    private static void clearBlock(Customer customer) {
        customer.setBlockReason(null);
        customer.setBlockedAt(null);
    }

    /** Grava já (para devolver version e datas atualizadas) e traduz a corrida de CPF/e-mail duplicados. */
    private Customer saveAndFlush(Customer customer) {
        try {
            return customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            throw ConstraintViolations.constraintName(e)
                    .map(UNIQUE_CONSTRAINTS::get)
                    .<RuntimeException>map(BusinessException::new)
                    .orElse(e);
        }
    }
}
