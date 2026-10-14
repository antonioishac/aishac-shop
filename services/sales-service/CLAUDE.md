# sales-service

Dono dos contextos **Catálogo**, **Pedido** e **Pagamento**. Banco `aishac_sales` (usuário `sales_app`).

## Pacotes (ADR-025 + ADR-026)

```text
com.aishac.sales/
├── config/      # transversal: Clock, auditoria JPA, OpenAPI, correlação (entrada e RestClient de saída), máscara de logs
├── exception/   # transversal: ErrorCode, CommonErrorCode, GlobalExceptionHandler
├── entity/      # transversal: BaseEntity (UUID v7 + auditoria)
├── dto/         # transversal: PageResponse, ProblemResponse (documentação OpenAPI)
├── catalog/     # categorias, produtos, estoque, imagens (F01-030)
│   └── config · controller · dto · entity · exception · mapper · repository · service/impl · specification
├── order/       # pedido, itens, máquina de estados (F01-040) — mesma estrutura
└── payment/     # pagamento simulado, Strategy (F01-046) — mesma estrutura
```

## Fronteiras entre módulos

- `catalog` não conhece `order` nem `payment`.
- `order` usa o catálogo **só pelas interfaces `*Service`** do catálogo, com os DTOs e códigos de erro dele (nunca repository, entity, mapper, storage ou helpers); sem FK entre tabelas de módulos (o catálogo poderá ser extraído, IF-010). O ArchUnit verifica.

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
