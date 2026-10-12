# sales-service

Dono dos contextos **Catálogo**, **Pedido** e **Pagamento**. Banco `aishac_sales` (usuário `sales_app`).

## Pacotes (camadas por tipo, ADR-025)

```text
com.aishac.sales/
├── config/      # Clock, auditoria JPA, OpenAPI, correlação (entrada e RestClient de saída), máscara de logs
└── exception/   # ErrorCode, CommonErrorCode, GlobalExceptionHandler
```

As demais camadas (`controller`, `dto`, `entity`, `mapper`, `repository`, `service`, `specification`) nascem com as features, como no `customer-service`.

## Módulos (catálogo, pedido, pagamento)

- **Em aberto:** como os módulos `catalog`, `order` e `payment` se organizam dentro das camadas será decidido na spec F01-030 (ADR-025).
- Até lá: `catalog` não depende de `order` nem de `payment`, e não há FK entre tabelas de módulos diferentes (o catálogo poderá ser extraído, IF-010).

## Transversal

- `CorrelationIdFilter` e `CorrelationIdPropagationInterceptor` (repassa o ID em todo `RestClient` de saída).
- `GlobalExceptionHandler`: todo erro em ProblemDetail com `code`, `correlationId`, `timestamp`.
- Rotas com versão fixa: `@RequestMapping("/api/v1/...")`. Paginação com `Pageable` + `PageResponse`.
- Testes: `src/test/resources/application.properties` carrega os arquivos reais do `config-repo/` (perfil `local`). `*IT` estendem `AbstractIntegrationTest` (Testcontainers; exige Docker ativo).

## Regras do contexto

- Preço e total sempre calculados aqui; o cliente envia só `productId` e `quantity`.
- Baixa de estoque atômica (`UPDATE ... WHERE stock_quantity >= :qty`), travando produtos **em ordem crescente de ID**.
- Criação de pedido exige cabeçalho `Idempotency-Key`.
- Transições de status só pela máquina de estados (padrão State), sempre gravando `order_status_history`.
- No máximo um pagamento `APPROVED` por pedido.
- Cancelamento devolve o estoque na mesma transação.
- Chamadas ao customer-service via `CustomerClient` (HTTP Interface) com Circuit Breaker, Retry e TimeLimiter; falha → `503` em `ProblemDetail`.
- Imagens: ver rule `storage`.
