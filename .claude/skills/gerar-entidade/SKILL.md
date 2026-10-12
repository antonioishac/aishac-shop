---
name: gerar-entidade
description: Gera o esqueleto completo de um agregado/feature na arquitetura hexagonal do projeto (domínio, casos de uso, ports, entidade JPA, repositório, mappers MapStruct, controller, DTOs Records e test data builder). Use após migration e modelo aprovados.
argument-hint: "[servico] [modulo] [NomeDoAgregado]"
arguments: [service, module, name]
---

# Gerar feature `$name` em `$service` / `$module`

Leia a spec e o modelo de dados aprovados. Gere apenas o que a spec pede (não crie endpoints especulativos). Estrutura de referência: [reference.md](reference.md).

## Ordem
1. `domain/` — `$name` (agregado, sem anotações de framework), Value Objects, enum/sealed de status, exceções de domínio.
2. `application/port/in/` — interfaces de casos de uso (`Create$nameUseCase`…), comandos e consultas como Records.
3. `application/port/out/` — `Load${name}Port`, `Save${name}Port`.
4. `application/service/` — implementação dos casos de uso, `@Transactional`.
5. `adapter/out/persistence/` — `${name}JpaEntity`, `${name}JpaRepository`, `${name}PersistenceAdapter`, `${name}PersistenceMapper` (MapStruct).
6. `adapter/in/web/` — `${name}Controller` (OpenAPI completo), `dto/*Request`, `dto/*Response`, `${name}WebMapper`.
7. `src/test/.../${name}TestBuilder`.

## Checagens
- Rules `java-style`, `lombok`, `api-openapi`, `architecture-boundaries`.
- MapStruct com `componentModel = "spring"` e `unmappedTargetPolicy = ReportingPolicy.ERROR`.
- `./mvnw test` compila e o `ArchitectureTest` passa.
