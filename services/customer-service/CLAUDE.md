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

## Regras do contexto

- CPF é Value Object validado no domínio (dígitos verificadores); armazenado só com dígitos (`CHAR(11)`).
- E-mail único sem diferenciar maiúsculas.
- Cliente **nunca é excluído fisicamente**: só `INACTIVE` ou `BLOCKED` (D18).
- No máximo um endereço padrão por cliente (garantido também por índice único parcial).
- CPF e e-mail **mascarados nos logs**.

## Consumidores deste serviço

- `sales-service` (dados do cliente para o pedido) e `web-bff`.
- Toda mudança em endpoint consumido exige atualizar o **contrato produtor** (Spring Cloud Contract) e rodar `./mvnw verify`.
