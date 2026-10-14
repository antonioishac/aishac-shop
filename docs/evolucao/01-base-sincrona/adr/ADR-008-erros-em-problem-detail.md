# ADR-008 — Erros em ProblemDetail (RFC 9457) com catálogo de códigos

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D09 · F01-012 · F01-020 · F01-026 · F01-030 · F01-039 |

## Contexto
Clientes da API (BFF, frontend, outro serviço) precisam distinguir erros de forma estável, sem interpretar mensagens em texto. Cada framework e cada serviço tende a inventar o próprio formato.

## Decisão
Toda resposta de erro usa **`application/problem+json` (RFC 9457)**, com três propriedades fixas:
- `code`: código estável do catálogo, ex.: `CUSTOMER_NOT_FOUND`;
- `correlationId`: o mesmo do log;
- `timestamp`: vindo do `Clock`.

Erros de validação trazem `errors[]` com campo e mensagem. Erro inesperado vira `500 INTERNAL_ERROR` **sem detalhes internos** (sem stack trace nem mensagem da exceção). O `GlobalExceptionHandler`, que estende `ResponseEntityExceptionHandler`, é a implementação base de cada serviço; os códigos de domínio entram nos catálogos de erro de cada serviço (F01-026, F01-039).

Cada serviço tem a interface `ErrorCode` (código, mensagem padrão e status HTTP), o catálogo transversal `CommonErrorCode` e um catálogo por contexto (ex.: `CustomerErrorCode`). Códigos transversais:

| Código | Status | Quando |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Bean Validation, parâmetro de tipo inválido, `sort` inexistente ou valor rejeitado só pelo CHECK do banco (com o campo da API, nunca o nome da constraint) |
| `MALFORMED_REQUEST` | 400 | Corpo JSON ilegível |
| `FILE_TOO_LARGE` | 400 | Upload multipart acima do limite do servidor (`spring.servlet.multipart.*`, `MaxUploadSizeExceededException`). Só no `sales-service`, o único com upload |
| `ROUTE_NOT_FOUND` | 404 | Rota inexistente |
| `METHOD_NOT_ALLOWED` | 405 | Verbo não suportado pela rota |
| `CONCURRENT_MODIFICATION` | 409 | Lock otimista ou pessimista em conflito |
| `DATA_INTEGRITY_VIOLATION` | 409 | Outra violação de integridade não traduzida pelo service |
| `INTERNAL_ERROR` | 500 | Qualquer erro inesperado |
| `SERVICE_UNAVAILABLE` | 503 | Dependência obrigatória indisponível (ex.: banco fora do ar ou sem conexão) |

Referência viva: `customer-service`. O `sales-service` tem o mesmo catálogo transversal, incluindo `SERVICE_UNAVAILABLE` (banco indisponível: `CannotCreateTransactionException` e `DataAccessResourceFailureException` → 503), mais o `FILE_TOO_LARGE`, ambos desde o F01-030.

Catálogos por contexto implementados:

| Catálogo | Códigos |
|---|---|
| `CustomerErrorCode` (customer-service, F01-026) | ver o enum e a spec F01-020 |
| `CatalogErrorCode` (sales-service, `catalog.exception`, F01-030/F01-039) | `INVALID_IMAGE_TYPE` (400), `IMAGE_TOO_LARGE` (400), `CATEGORY_NOT_FOUND` (404), `PRODUCT_NOT_FOUND` (404), `IMAGE_NOT_FOUND` (404), `CATEGORY_NAME_ALREADY_EXISTS` (409), `SKU_ALREADY_EXISTS` (409), `CATEGORY_INACTIVE` (422), `INSUFFICIENT_STOCK` (422), `IMAGE_LIMIT_EXCEEDED` (422), `IMAGE_STORAGE_UNAVAILABLE` (503) |

Os dois limites de tamanho de imagem são distintos: `IMAGE_TOO_LARGE` é a regra de negócio (`aishac.storage.max-file-size`, padrão 5 MB), conferida pelo service; `FILE_TOO_LARGE` é o teto técnico do multipart, acima do qual o Spring recusa a requisição antes de ela chegar ao controller.

Erros 4xx são logados em WARN com mensagem segura (sem o valor rejeitado); 5xx em ERROR com stack trace, que nunca vai na resposta.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Formato próprio (`{error, message}`) | Livre | Reinventa um padrão; cada cliente trata de um jeito |
| Erro padrão do Spring Boot (`/error`) | Zero código | Sem código estável; vaza detalhes se mal configurado |
| **ProblemDetail + `code`** | Padrão aberto, suportado nativamente pelo Spring | Exige catálogo de códigos mantido |

## Consequências
- **Positivas:** o BFF e o frontend tratam erros por `code`; o `correlationId` liga a resposta ao log.
- **Negativas / custos:** manter o catálogo de códigos sincronizado com o contrato OpenAPI.
- **O que observar:** códigos genéricos demais (`BAD_REQUEST`) onde caberia um código de domínio.
