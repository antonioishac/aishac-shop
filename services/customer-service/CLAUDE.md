# customer-service

Dono do contexto **Clientes**: cadastro, status e endereços. Banco `aishac_customer` (usuário `customer_app`).

## Pacotes (camadas por tipo, ADR-025)

```text
com.aishac.customer/
├── config/          # Clock, auditoria JPA, OpenAPI, CorrelationIdFilter, máscara de logs
├── controller/      # CustomerController, CustomerAddressController (sem regra de negócio)
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
