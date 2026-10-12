---
paths:
  - "services/**/src/test/**"
  - "web/**/*.test.{ts,tsx}"
  - "web/**/e2e/**"
---

# Testes

## Geral
- Pirâmide definida no `CLAUDE.md` raiz. Regras de negócio (services) são testadas **sem Spring**, com o repository mockado e o mapper real (`Mappers.getMapper`).
- Nome do método descreve o comportamento: `shouldRejectOrderWhenStockIsInsufficient`.
- Estrutura Given / When / Then visível (comentários ou blocos).
- **AssertJ** para asserções (nada de `assertEquals` do JUnit). Nos unitários de service, mocke o repository (Mockito); nunca mocke o que você não possui em teste de integração (ex.: `RestClient`, repositórios Spring Data).
- Use os builders das entidades (`Customer.builder()...build()`) ou fixtures de teste, em vez de montar objetos campo a campo em cada teste.
- `Clock` fixo nos testes que dependem de tempo.

## Integração (`*IT`)
- `@SpringBootTest` + **Testcontainers** com `@ServiceConnection` (PostgreSQL) e LocalStack para S3 (token via `LOCALSTACK_AUTH_TOKEN` do ambiente).
- Containers reutilizados por classe base abstrata; nada de H2.
- Chamadas HTTP a outros serviços: WireMock ou stubs do Spring Cloud Contract.

## Contrato
- Produtor (customer-service, sales-service): contratos em `src/test/resources/contracts/`, geram testes e stubs.
- Consumidor (sales-service → customer, web-bff → ambos): Stub Runner com os stubs publicados localmente.

## Concorrência (Virtual Threads)
- Use `Executors.newVirtualThreadPerTaskExecutor()` + `CountDownLatch` para disparar requisições simultâneas.
- Cenários obrigatórios: último item em estoque (só 1 vence), pedidos cruzados com vários itens (sem deadlock), mesma `Idempotency-Key` em paralelo (1 pedido).

## Arquitetura
- Uma classe `ArchitectureTest` por serviço com as regras da rule `architecture-boundaries`.

## Frontend
- Vitest + Testing Library: teste comportamento visível (texto, papel ARIA), não detalhes de implementação.
- Mock de rede com MSW.
- Playwright para fluxos E2E, contra o ambiente local que o usuário subiu.

## Execução
- O Claude pode rodar `./mvnw test` e `./mvnw verify` (Testcontainers permitido).
- O Claude **não** sobe containers fora do Testcontainers.
