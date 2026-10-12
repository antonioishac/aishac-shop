---
paths:
  - "services/**/controller/**"
  - "services/**/dto/**"
  - "services/web-bff/**/*.java"
  - "specs/**/contract.yaml"
---

# APIs REST e OpenAPI

## Convenções
- Prefixo e versão fixos no `@RequestMapping`: `/api/v1/...` (ADR-025). Recursos no plural, em inglês, kebab-case (`/product-images`).
- Verbos HTTP corretos: `POST` cria (201 + `Location`), `GET` lê, `PUT` substitui, `PATCH` altera parcialmente, `DELETE` remove (204).
- Request e response são **Records** em `dto/`, sufixos `Request` / `Response`. Nunca exponha entidade JPA.
- Validação com Bean Validation (`@Valid`, `@NotBlank`, `@Size`…) no Request; regra de negócio no service.
- **Dinheiro como string decimal** (`"129.90"`). Datas em ISO-8601 com fuso (`OffsetDateTime`/`Instant`).
- IDs são UUID em string.

## Paginação, ordenação e filtros
- `Pageable` do Spring Data no controller (`@PageableDefault`, `@ParameterObject`): `page` (0-based), `size` (padrão 20, **máximo 100** por `spring.data.web.pageable.max-page-size`), `sort=campo,asc|desc`. Propriedade de ordenação inexistente → 400.
- Resposta paginada padronizada pelo `dto/PageResponse.of(page)`: `{ "content": [...], "page": { "number", "size", "totalElements", "totalPages" } }`.
- Filtros como query params nomeados; nada de linguagem de consulta livre.

## Erros
- Sempre `ProblemDetail` (RFC 9457), gerado pelo `GlobalExceptionHandler` (`@RestControllerAdvice`) em `exception/`.
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
