---
paths:
  - "services/web-bff/**"
---

# BFF (`web-bff`)

## Papel
- Contrato desenhado **para as telas** do `aishac-shop-web`: `/api/v1/store/**` e `/api/v1/admin/**`.
- **Só compõe e adapta.** Proibido: regra de negócio, cálculo de preço/total, validação de estoque, decisão de status, acesso a banco, cache com estado próprio.
- Pergunta-teste: "se outro frontend existisse, essa lógica teria que ser duplicada?" Se sim, ela pertence a um serviço.

## Chamadas aos serviços
- HTTP Interfaces por serviço em `client/`, com Resilience4j (Circuit Breaker, Retry só em operações idempotentes, TimeLimiter).
- **Nunca** aplique Retry em `POST` de criação de pedido sem a `Idempotency-Key` propagada.
- Propague cabeçalhos: ID de correlação e `Idempotency-Key`.

## Agregação
- Várias chamadas independentes → `StructuredTaskScope` (em paralelo, cancelando as demais em falha obrigatória).
- Classifique cada fonte como **obrigatória** (falha → `ProblemDetail`) ou **opcional** (falha → campo nulo + `warnings[]` na resposta).

## DTOs
- Records próprios do BFF, por tela. Não reexporte o DTO do serviço, mesmo que idêntico hoje.
- Erros dos serviços (`ProblemDetail`) são traduzidos para o formato do BFF preservando `code` e `correlationId`.

## Uploads
- Multipart repassado em **streaming** ao sales-service; limite de tamanho igual ou menor que o do serviço.
