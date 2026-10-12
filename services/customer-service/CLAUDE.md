# customer-service

Dono do contexto **Clientes**: cadastro, status e endereços. Banco `customers_db` (usuário `customer_app`).

## Pacotes

```text
com.aishac.customer/
├── customer/
│   ├── domain/                  # Customer, Cpf (Value Object), Email, CustomerStatus, Address
│   ├── application/             # casos de uso + ports (in/out)
│   ├── adapter/in/web/          # controllers, DTOs (Records), mapper web
│   └── adapter/out/persistence/ # @Entity, Spring Data, mapper JPA
└── infrastructure/              # config, exception handler, OpenAPI, logging
```

## Infraestrutura transversal (`infrastructure/`)

- `CorrelationIdFilter`: `X-Correlation-Id` (recebido ou gerado) no MDC e na resposta.
- `GlobalExceptionHandler`: todo erro em ProblemDetail com `code`, `correlationId`, `timestamp`.
- `SensitiveDataMaskingCustomizer`: mascara CPF/e-mail em todo valor do log JSON (registrado no `config-repo`).
- Versionamento nativo do Spring 7: mapeie `@RequestMapping("/api/{version}/...")` + `@GetMapping(version = "1")`.
- `Clock` injetável; OpenAPI em `/v3/api-docs`.
- Testes: `src/test/resources/application.properties` carrega os arquivos reais do `config-repo/` (perfil `local`), sem Config Server. `*IT` estendem `AbstractIntegrationTest` (PostgreSQL 18.4 via Testcontainers; exige Docker ativo).

## Regras do contexto

- CPF é Value Object validado no domínio (dígitos verificadores); armazenado só com dígitos (`CHAR(11)`).
- E-mail único sem diferenciar maiúsculas.
- Cliente **nunca é excluído fisicamente**: só `INACTIVE` ou `BLOCKED` (D18).
- No máximo um endereço padrão por cliente (garantido também por índice único parcial).
- CPF e e-mail **mascarados nos logs**.

## Consumidores deste serviço

- `sales-service` (dados do cliente para o pedido) e `web-bff`.
- Toda mudança em endpoint consumido exige atualizar o **contrato produtor** (Spring Cloud Contract) e rodar `./mvnw verify`.
