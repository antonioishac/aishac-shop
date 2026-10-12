# sales-service

Dono dos contextos **Catálogo**, **Pedido** e **Pagamento**. Banco `sales_db` (usuário `sales_app`).

## Pacotes

```text
com.aishac.sales/
├── catalog/    # categorias, produtos, estoque, imagens (S3)
├── order/      # pedido (agregado raiz), itens, máquina de estados, histórico
├── payment/    # Strategy: PixPayment, CardPayment, BoletoPayment (simulados)
└── infrastructure/
```

Cada módulo segue `domain/`, `application/`, `adapter/in/web/`, `adapter/out/...`.

## Fronteiras entre módulos

- `order` usa o catálogo **apenas por um port** do próprio `order` (ex.: `ProductCatalogPort`); nunca importa classes de `catalog.domain` ou `catalog.adapter`.
- `catalog` não conhece `order`. O ArchUnit garante isso.
- Motivo: o catálogo poderá ser extraído para outro serviço no futuro (IF-010).

## Infraestrutura transversal (`infrastructure/`)

- `CorrelationIdFilter`: `X-Correlation-Id` (recebido ou gerado) no MDC e na resposta. `CorrelationIdPropagationInterceptor` o repassa em todo `RestClient` de saída.
- `GlobalExceptionHandler`: todo erro em ProblemDetail com `code`, `correlationId`, `timestamp`.
- `SensitiveDataMaskingCustomizer`: mascara CPF/e-mail em todo valor do log JSON (registrado no `config-repo`).
- Versionamento nativo do Spring 7: mapeie `@RequestMapping("/api/{version}/...")` + `@GetMapping(version = "1")`.
- `Clock` injetável; OpenAPI em `/v3/api-docs`.
- Testes: `src/test/resources/application.properties` carrega os arquivos reais do `config-repo/` (perfil `local`), sem Config Server. `*IT` estendem `AbstractIntegrationTest` (PostgreSQL 18.4 via Testcontainers; exige Docker ativo).

## Regras do contexto

- Preço e total sempre calculados aqui; o cliente envia só `productId` e `quantity`.
- Baixa de estoque atômica (`UPDATE ... WHERE stock_quantity >= :qty`), travando produtos **em ordem crescente de ID**.
- Criação de pedido exige cabeçalho `Idempotency-Key`.
- Transições de status só pela máquina de estados (padrão State), sempre gravando `order_status_history`.
- No máximo um pagamento `APPROVED` por pedido.
- Cancelamento devolve o estoque na mesma transação.
- Chamadas ao customer-service via `CustomerClient` (HTTP Interface) com Circuit Breaker, Retry e TimeLimiter; falha → `503` em `ProblemDetail`.
- Imagens: ver rule `storage`.
