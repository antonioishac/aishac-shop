# Changelog — customer-service

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento semântico por serviço (ver `CONTRIBUTING.md`).

## [Unreleased]

### Adicionado
- Contrato produtor do `order-data` para o sales-service (F01-025): Spring Cloud Contract com sete cenários (endereço padrão, endereço escolhido, sem endereço, cliente bloqueado, cliente desativado, `CUSTOMER_NOT_FOUND`, `ADDRESS_NOT_FOUND`), testes gerados no `test` e `customer-service-stubs.jar` para o Stub Runner.
- Cadastro de clientes (F01-020 a F01-024, F01-026): CPF validado e único, e-mail único sem diferenciar maiúsculas, status `ACTIVE`/`BLOCKED`/`INACTIVE` sem exclusão, lock otimista, até 10 endereços com um padrão, listagem paginada com filtros (nome, e-mail, CPF, status) e `order-data` para o sales-service.
- Migrations `V1__create_customers` e `V2__create_customer_addresses`.
- Banco fora do ar responde `503 SERVICE_UNAVAILABLE` e tira o serviço de *readiness*.
- Esqueleto do serviço: configuração pelo Config Server, logs JSON com máscara de CPF/e-mail, ID de correlação, ProblemDetail, versionamento de API, OpenAPI, Dockerfile e gates de qualidade.
