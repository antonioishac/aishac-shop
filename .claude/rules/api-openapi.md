---
paths:
  - "services/**/adapter/in/web/**"
  - "services/web-bff/**/*.java"
  - "specs/**/contract.yaml"
---

# APIs REST e OpenAPI

## Convenções
- Prefixo e versão: `/api/v1/...`. Recursos no plural, em inglês, kebab-case (`/product-images`).
- Verbos HTTP corretos: `POST` cria (201 + `Location`), `GET` lê, `PUT` substitui, `PATCH` altera parcialmente, `DELETE` remove (204).
- Request e response são **Records** em `adapter/in/web/dto`, sufixos `Request` / `Response`. Nunca exponha entidade JPA nem modelo de domínio.
- Validação com Bean Validation (`@Valid`, `@NotBlank`, `@Size`…) no Request; regra de negócio no domínio.
- **Dinheiro como string decimal** (`"129.90"`). Datas em ISO-8601 com fuso (`OffsetDateTime`/`Instant`).
- IDs são UUID em string.

## Paginação, ordenação e filtros
- Parâmetros: `page` (0-based), `size` (padrão 20, **máximo 100**), `sort=campo,asc|desc`.
- Resposta paginada padronizada: `{ "content": [...], "page": { "number", "size", "totalElements", "totalPages" } }`.
- Filtros como query params nomeados; nada de linguagem de consulta livre.

## Erros
- Sempre `ProblemDetail` (RFC 9457), gerado pelo `@RestControllerAdvice` central.
- Campos extras: `code` (do catálogo de erros, ex.: `INSUFFICIENT_STOCK`), `correlationId`, e `errors[]` para validação de campos.
- Mapeamento: validação 400 · não encontrado 404 · conflito/regra de negócio 409 ou 422 · dependência indisponível 503.

## Idempotência
- `POST` que cria pedido exige `Idempotency-Key` (string até 64). Mesma chave + mesmo corpo → mesma resposta; mesma chave + corpo diferente → 422.

## Documentação obrigatória
Todo endpoint tem:
- `@Tag` no controller; `@Operation(summary, description)`;
- `@ApiResponse` para **cada** status possível (incluindo erros, com schema `ProblemDetail`);
- `@Parameter` em path/query/header params;
- exemplos (`@ExampleObject`) nos casos não triviais.

O contrato em `specs/.../contract.yaml` é a fonte; o código deve gerar um OpenAPI equivalente.
