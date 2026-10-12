# Plano técnico — F01-020 (implementação F01-021 a F01-024 e F01-026)

## Visão geral
Cadastro de clientes no customer-service, em **camadas por tipo** (ADR-025). O controller só valida e delega; as regras de negócio da spec ficam nos services (`CustomerServiceImpl`, `CustomerAddressServiceImpl`) e no banco (UNIQUE, CHECK, EXCLUDE adiável). Endereços são entidades filhas de `Customer` (cascade), sem repositório próprio. A listagem usa `Specification` + `Pageable` e não toca a coleção de endereços (sem N+1).

> Versão anterior deste plano usava hexagonal leve (ADR-004). Reescrito após a mudança de arquitetura pedida pelo usuário.

## Arquivos
| Camada | Arquivo | Ação |
|---|---|---|
| entity | `BaseEntity` (UUID v7, auditoria), `Customer`, `CustomerAddress`, `CustomerStatus` | criar |
| repository | `CustomerRepository` (`findWithAddressesById`, `findForAddressChange` com trava, `existsBy...`) | criar |
| specification | `CustomerSpecifications.withFilters(name, email, cpf, status)` | criar |
| mapper | `CustomerMapper` (MapStruct) | criar |
| service | `CustomerService`, `CustomerAddressService`, `CpfValidator`, `CustomerRules`; `impl/CustomerServiceImpl`, `impl/CustomerAddressServiceImpl` | criar |
| dto | `CustomerRequest`, `CustomerUpdateRequest`, `BlockCustomerRequest`, `CustomerResponse`, `AddressRequest`, `AddressResponse`, `OrderDataResponse`, `PageResponse` | criar |
| controller | `CustomerController` (inclui `order-data`), `CustomerAddressController` | criar |
| exception | `ErrorCode`, `CommonErrorCode`, `CustomerErrorCode`, `BusinessException`, `NotFoundException`, `ProblemDetailFactory`, `GlobalExceptionHandler` | criar |
| config | `ClockConfiguration` (precisão de µs), `JpaAuditingConfiguration`, `OpenApiConfiguration` (cabeçalho de correlação), `CorrelationIdFilter`, máscara de logs | criar |
| resources/db/migration | `V1__create_customers.sql`, `V2__create_customer_addresses.sql` | criar |
| test | unitários dos services, `@WebMvcTest`, handler *standalone*, `CustomerApiIT`, `CustomerConcurrencyIT`, `ArchitectureTest` | criar |

## Padrões aplicados
| Padrão | Onde | Por quê |
|---|---|---|
| Service layer (interface + impl) | `service/` | Regras de negócio num só lugar; controller fino |
| Specification | `CustomerSpecifications` | Filtros combináveis sem explosão de `findByXAndY` |
| Builder | entidades (`@SuperBuilder`) | Criação legível em service e testes |
| Catálogo de erros | `ErrorCode` + enums | Código estável e status HTTP definidos por erro |

## Resiliência e concorrência
- **Dados e status:** `@Version` em `Customer`; o `PUT` traz a `version` lida, e divergência gera `CONCURRENT_MODIFICATION`. Corrida entre transações gera `OptimisticLockingFailureException`, traduzida para o mesmo código.
- **Endereços:** `findForAddressChange` com `PESSIMISTIC_FORCE_INCREMENT` trava o cliente e incrementa a versão na hora. Inclusões simultâneas viram sequenciais, e o 11º endereço recebe `ADDRESS_LIMIT_EXCEEDED` de forma determinística.
- **CPF/e-mail duplicados:** checagem prévia para mensagem amigável; a garantia em corrida é a UNIQUE, traduzida no service pelo nome da constraint.
- **CHECK violado** (ex.: contagem de caracteres diferente entre Java e PostgreSQL): 400 `VALIDATION_ERROR` no handler.
- Sem chamadas externas: Resilience4j não se aplica.

## Configuração nova
| Propriedade | local | dev | prod |
|---|---|---|---|
| `spring.data.web.pageable.default-page-size` / `max-page-size` (comum) | 20 / 100 | 20 / 100 | 20 / 100 |
| `spring.datasource.username` | `${DB_USERNAME:customer_app}` | idem | idem |

## Dependências novas
Nenhuma. UUID v7 pelo próprio Hibernate (`@UuidGenerator(style = VERSION_7)`).

## Riscos
- Busca por nome não ignora acentos ("Joao" não acha "João"): fora de escopo.
- Entidades com `@Setter`: as invariantes dependem de toda alteração passar pelos services (e do banco).
