# Plano técnico — F01-020 (implementação F01-021 a F01-024 e F01-026)

## Visão geral
Feature `customer` no customer-service, em hexagonal leve (ADR-004). `Customer` é o agregado raiz e contém os `Address`; toda regra de status, limite e endereço padrão vive no domínio, sem Spring. Comandos carregam o agregado inteiro; consultas de listagem e detalhe usam um **modelo de leitura** (`CustomerView`), sem endereços, para evitar N+1. O banco reforça as invariantes (UNIQUE, CHECK, EXCLUDE adiável) e as violações concorrentes são traduzidas para os códigos do catálogo.

## Arquivos
| Camada | Arquivo | Ação |
|---|---|---|
| shared/domain | `DomainException`, `ErrorCode`, `ErrorCategory` | criar: base das exceções e catálogo (ADR-008) |
| shared/application | `IdGenerator`, `PageQuery`, `PageResult` | criar |
| customer/domain | `Customer`, `Address`, `AddressData`, `Cpf`, `Email`, `Phone`, `CustomerStatus`, `BrazilianState`, `CustomerErrorCode`, exceções | criar |
| customer/application/port/in | `RegisterCustomerUseCase`, `UpdateCustomerUseCase`, `ChangeCustomerStatusUseCase`, `ManageAddressesUseCase`, `CustomerQueries` + comandos (Records) | criar |
| customer/application/port/out | `LoadCustomerPort`, `SaveCustomerPort`, `CustomerQueryPort`, `CustomerView`, `CustomerFilter`, `OrderDataView` | criar |
| customer/application/service | `CustomerService`, `CustomerAddressService`, `CustomerQueryService` | criar (`@Transactional`) |
| customer/adapter/out/persistence | `CustomerJpaEntity`, `CustomerAddressJpaEntity`, `CustomerJpaRepository`, `CustomerPersistenceAdapter`, `CustomerPersistenceMapper` (MapStruct), `CustomerSpecifications` | criar |
| customer/adapter/in/web | `CustomerController`, `CustomerAddressController`, `OrderDataController`, `CustomerWebMapper` (MapStruct), `dto/*` | criar |
| infrastructure | `UuidV7Generator`; `GlobalExceptionHandler` (DomainException, lock otimista, validação de parâmetros) | criar / alterar |
| resources/db/migration | `V1__create_customers.sql`, `V2__create_customer_addresses.sql` | criar |
| test | unitários de domínio e serviço, fatia web, `CustomerApiIT`, `CustomerTestBuilder` | criar |

## Padrões aplicados
| Padrão | Onde | Por quê |
|---|---|---|
| Aggregate (DDD) | `Customer` + `Address` | Limite de 10 e padrão único são invariantes do cliente |
| Value Object | `Cpf`, `Email`, `Phone` | Validação e normalização num só lugar (RN-02, RN-04) |
| Factory method | `Customer.register` / `Customer.restore` | Separa criação (regras) de reconstituição (persistência) |
| Adapter / Ports | `*PersistenceAdapter`, controllers | Domínio isolado da infraestrutura |
| CQRS leve | `CustomerQueryPort` + `CustomerView` | Leituras sem carregar endereços (sem N+1) |

## Resiliência e concorrência
- Lock otimista na raiz (`@Version` em `customers`): o comando traz a `version` lida; divergência → `CONCURRENT_MODIFICATION`. Corrida entre transações → `OptimisticLockingFailureException` → mesmo código.
- Operações de endereço alteram `updated_at` da raiz, o que incrementa a `version` e serializa inclusões concorrentes (limite de 10).
- CPF/e-mail duplicados: checagem prévia para mensagem amigável; garantia pela UNIQUE, traduzida no adapter pelo nome da constraint.
- Endereço padrão: EXCLUDE adiável no banco (verificada no COMMIT).
- Sem chamadas externas: não se aplica Resilience4j.

## Configuração nova
Nenhuma.

## Dependências novas
Nenhuma. UUID v7 gerado por `UuidV7Generator` (48 bits de tempo do `Clock` + aleatório), sem biblioteca.

## Riscos
- `CHAR(11)`/`CHAR(8)` com `ddl-auto=validate` exigem mapeamento explícito do tipo (`@JdbcTypeCode(SqlTypes.CHAR)`).
- Busca por nome não ignora acentos ("Joao" não acha "João"): fora de escopo, registrado na spec.
