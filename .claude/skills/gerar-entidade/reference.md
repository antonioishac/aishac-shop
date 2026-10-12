# Estrutura de referência: `customer-service`

Implementação completa da spec F01-020, no padrão em camadas por tipo (ADR-025). Copie a forma, não o conteúdo.

```text
com.aishac.customer/
├── config/          ClockConfiguration, JpaAuditingConfiguration, OpenApiConfiguration,
│                    CorrelationIdFilter, SensitiveDataMasker(+Customizer)
├── controller/      CustomerController, CustomerAddressController
├── dto/             CustomerRequest, CustomerUpdateRequest, BlockCustomerRequest, CustomerResponse,
│                    AddressRequest, AddressResponse, OrderDataResponse, PageResponse
├── entity/          BaseEntity, Customer, CustomerAddress, CustomerStatus
├── exception/       ErrorCode, CommonErrorCode, CustomerErrorCode, BusinessException,
│                    NotFoundException, ProblemDetailFactory, GlobalExceptionHandler
├── mapper/          CustomerMapper
├── repository/      CustomerRepository
├── service/         CustomerService, CustomerAddressService, CpfValidator, CustomerRules
│   └── impl/        CustomerServiceImpl, CustomerAddressServiceImpl
└── specification/   CustomerSpecifications
```

## Esqueletos

```java
// controller: só recebe, valida e delega
@Tag(name = "Customers", description = "...")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Listar clientes")
    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.of(customerService.list(name, pageable));
    }
}
```

```java
// service/impl: regras de negócio e transação
@Service
@RequiredArgsConstructor
class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(String name, Pageable pageable) {
        var specification = CustomerSpecifications.withFilters(name);
        return customerRepository.findAll(specification, pageable).map(customerMapper::toResponse);
    }
}
```

```java
// entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
```
