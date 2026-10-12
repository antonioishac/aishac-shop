# ADR-008 — Erros em ProblemDetail (RFC 9457) com catálogo de códigos

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D09 · F01-012 · F01-026 · F01-039 |

## Contexto
Clientes da API (BFF, frontend, outro serviço) precisam distinguir erros de forma estável, sem interpretar mensagens em texto. Cada framework e cada serviço tende a inventar o próprio formato.

## Decisão
Toda resposta de erro usa **`application/problem+json` (RFC 9457)**, com três propriedades fixas:
- `code`: código estável do catálogo, ex.: `CUSTOMER_NOT_FOUND`;
- `correlationId`: o mesmo do log;
- `timestamp`: vindo do `Clock`.

Erros de validação trazem `errors[]` com campo e mensagem. Erro inesperado vira `500 INTERNAL_ERROR` **sem detalhes internos** (sem stack trace nem mensagem da exceção). O `GlobalExceptionHandler`, que estende `ResponseEntityExceptionHandler`, é a implementação base de cada serviço; os códigos de domínio entram nos catálogos de erro de cada serviço (F01-026, F01-039).

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
