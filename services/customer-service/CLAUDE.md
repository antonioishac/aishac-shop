# customer-service

Dono do contexto **Clientes**: cadastro, status e endereços. Banco `aishac_customer` (usuário `customer_app`).

## Pacotes (camadas por tipo, ADR-025)

```text
com.aishac.customer/
├── config/          # Clock, auditoria JPA, OpenAPI, CorrelationIdFilter, máscara de logs
├── controller/      # CustomerController, CustomerAddressController, CustomerOrderDataController (sem regra de negócio)
├── dto/             # Records de request/response, PageResponse
├── entity/          # BaseEntity, Customer, CustomerAddress, CustomerStatus
├── exception/       # ErrorCode, CommonErrorCode, CustomerErrorCode, GlobalExceptionHandler
├── mapper/          # CustomerMapper (MapStruct)
├── repository/      # CustomerRepository
├── service/         # interfaces + CpfValidator, CustomerRules
│   └── impl/        # CustomerServiceImpl, CustomerAddressServiceImpl (regras de negócio)
└── specification/   # CustomerSpecifications (filtros da listagem)
```

É a **referência viva** do padrão do projeto (skill `gerar-entidade`).

## Transversal

- `CorrelationIdFilter`: `X-Correlation-Id` (recebido ou gerado) no MDC, na resposta e no ProblemDetail.
- `GlobalExceptionHandler`: todo erro em ProblemDetail com `code`, `correlationId`, `timestamp` (e `errors[]` em validação).
- `SensitiveDataMaskingCustomizer`: mascara CPF/e-mail em todo valor do log JSON (registrado no `config-repo`).
- Rotas com versão fixa: `@RequestMapping("/api/v1/...")`. Paginação com `Pageable` + `PageResponse`.
- Testes: `src/test/resources/application.properties` carrega os arquivos reais do `config-repo/` (perfil `local`), sem Config Server. `*IT` estendem `AbstractIntegrationTest` (PostgreSQL 18.4 via Testcontainers; exige Docker ativo).

## Regras do contexto

- CPF validado no service (`CpfValidator`, dígitos verificadores); armazenado só com dígitos (`CHAR(11)`).
- E-mail único sem diferenciar maiúsculas.
- Cliente **nunca é excluído fisicamente**: só `INACTIVE` ou `BLOCKED` (D18).
- No máximo 10 endereços e um padrão por cliente (padrão também garantido por *exclusion constraint* adiável). Alterações de endereço travam o cliente (`findForAddressChange`, `PESSIMISTIC_FORCE_INCREMENT`).
- CPF e e-mail **mascarados nos logs**.

## Consumidores deste serviço

- `sales-service` (dados do cliente para o pedido) e `web-bff`.
- Toda mudança em endpoint consumido exige atualizar o **contrato produtor** (Spring Cloud Contract) e rodar `./mvnw verify`.
- Contratos em `src/test/resources/contracts/<consumidor>/` (YAML); cada pasta tem a sua classe base em `src/test/java/.../contract/` (`baseClassMappings` no pom). A base é uma fatia `@WebMvcTest` com o service mockado; os ids dos contratos são os da base.
- Os testes gerados (`*ContractTest`) rodam no `test`; o `package` gera o `customer-service-stubs.jar`. A geração é incremental: depois de mexer no pom ou na base, use `./mvnw clean test`.
- O plugin **ignora com um WARNING** o YAML que não consegue ler (o build fica verde sem o teste e sem o stub). O `ContractGenerationTest` compara o número de contratos com o de testes gerados; pasta de consumidor nova entra nele.
- Contratos com a mesma URL e query diferente usam `priority` (menor vence no WireMock). O matcher `absent` de query não funciona em YAML (o teste gerado envia `param=null`).
- O ProblemDetail não traz `type` quando é `about:blank`: não o coloque no contrato.
- O Spring Cloud Contract saiu do BOM `spring-cloud-dependencies` na 2025.1.x: versão própria em `spring-cloud-contract.version` (BOM e plugin).
- Como o sales-service obtém os stubs (Stub Runner com `install` local ou lendo os contratos do monorepo, e o gatilho da CI) é decidido no F01-044.
