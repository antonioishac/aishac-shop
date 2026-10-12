---
name: gerar-entidade
description: Gera o esqueleto completo de uma feature nas camadas por tipo do projeto (entity, repository, specification, mapper MapStruct, service com interface e impl, DTOs Records, controller, códigos de erro e testes). Use após migration e modelo aprovados.
argument-hint: "[servico] [NomeDaEntidade]"
arguments: [service, name]
---

# Gerar feature `$name` em `$service`

Leia a spec e o modelo de dados aprovados. Gere apenas o que a spec pede (não crie endpoints especulativos). Referência real e completa: o `customer-service` (ver [reference.md](reference.md)).

## Ordem (rule `architecture-boundaries`)
1. `entity/` — `$name extends BaseEntity` (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder`, `@Version` se for raiz de concorrência), enums persistidos.
2. `repository/` — `${name}Repository extends JpaRepository<$name, UUID>, JpaSpecificationExecutor<$name>`; `@EntityGraph` para carregar coleções.
3. `specification/` — `${name}Specifications.withFilters(...)` para os filtros da listagem.
4. `exception/` — códigos novos no catálogo do contexto (`XxxErrorCode`: código, mensagem padrão, status HTTP).
5. `dto/` — `*Request` (Bean Validation + normalização simples no construtor compacto) e `*Response` (Records).
6. `mapper/` — `${name}Mapper` MapStruct (`componentModel = "spring"`): entidade → resposta; cópia de campos simples request → entidade.
7. `service/` — interface `${name}Service`; `service/impl/${name}ServiceImpl` package-private com **todas as regras de negócio**, `@Transactional` por método.
8. `controller/` — `${name}Controller`: `@Valid`, uma chamada ao service por endpoint, `Pageable` + `PageResponse` nas listagens, OpenAPI completo.
9. Testes: unitário do `ServiceImpl` (repository mockado, mapper real), `@WebMvcTest` do controller, `*IT` do fluxo.

## Checagens
- Rules `java-style`, `lombok`, `api-openapi`, `architecture-boundaries`, `database`.
- MapStruct com `unmappedTargetPolicy = ERROR` (já configurado no pom).
- `./mvnw test` compila e o `ArchitectureTest` passa.
