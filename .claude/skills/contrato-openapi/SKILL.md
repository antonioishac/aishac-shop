---
name: contrato-openapi
description: Cria ou valida o contrato OpenAPI de uma feature e as anotações correspondentes no controller, e gera os contratos Spring Cloud Contract quando o endpoint é consumido por outro serviço ou pelo BFF. Use ao criar ou mudar qualquer endpoint.
argument-hint: "[caminho do contract.yaml ou do controller]"
---

# Contrato OpenAPI

Siga a rule `api-openapi`.

## Criar ou alterar
1. Edite `specs/.../contract.yaml` primeiro (OpenAPI 3.1): paths, schemas, exemplos, respostas de erro com `ProblemDetail`.
2. Reflita no controller: `@Tag`, `@Operation`, `@ApiResponse` por status, `@Parameter`, `@ExampleObject`.
3. Mudança **incompatível** (remover campo, mudar tipo, tornar obrigatório) em endpoint consumido → nova versão de rota ou estratégia aprovada pelo usuário; nunca quebre consumidor silenciosamente.

## Validar
- Suba o contexto em teste e compare o `/v3/api-docs` gerado com o `contract.yaml` (paths, métodos, status, campos obrigatórios).
- Liste divergências para o usuário.

## Endpoint consumido por outro componente
| Produtor | Consumidores |
|---|---|
| customer-service | sales-service, web-bff |
| sales-service | web-bff |

- No produtor: contrato em `src/test/resources/contracts/<consumidor>/` (Groovy ou YAML), cobrindo sucesso e erros relevantes.
- No consumidor: teste com Stub Runner contra o stub.
- Contrato do BFF muda → avisar que o frontend precisa de `npm run generate:api`.
