# ADR-025 — Arquitetura em camadas por tipo (substitui a hexagonal)

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | Decisão do usuário após a primeira feature (F01-020) · substitui ADR-004 e ADR-022 · ajusta ADR-003 e ADR-005 |

## Contexto
A primeira feature (cadastro de cliente) foi construída em hexagonal leve por feature (ADR-004): domínio sem framework, ports de entrada e saída, adapters e mapeamento domínio ↔ JPA ↔ DTO. Na revisão, o resultado ficou confuso para o dono do projeto: muitas classes e indireções para um CRUD com regras, e um padrão diferente do que ele já usa e mantém em outros projetos (`cactus-erp/cadastro-base-service`). Para um projeto de estudo, a clareza e a familiaridade do código pesam mais que o isolamento máximo do domínio.

## Decisão
Organizar cada serviço em **pacotes por tipo**, no padrão do `cadastro-base-service`:

```text
config · controller · dto · entity · exception · mapper · repository · service (+ impl) · specification
```

- **Controller** sem regra de negócio: valida o DTO (`@Valid`), chama o service e devolve a resposta.
- **Service** (interface + implementação package-private em `service/impl`) concentra regras de negócio e transações.
- **Entity** é a entidade JPA/Hibernate (Lombok `@Getter/@Setter/@SuperBuilder`), com `BaseEntity` (UUID v7 e auditoria de datas). Não existe mais modelo de domínio separado.
- **Exception:** `ErrorCode` com status HTTP, catálogos por contexto e `GlobalExceptionHandler` com `@RestControllerAdvice` + `@ExceptionHandler`.
- **Paginação** com `Pageable` do Spring Data e envelope `PageResponse`; filtros com `Specification`.
- **Versão de API fixa no caminho** (`@RequestMapping("/api/v1/...")`), no lugar do versionamento nativo do Spring 7 (substitui o ADR-022).
- Sem `package-info.java` e sem anotações JSpecify.
- Fronteiras verificadas pelo `ArchitectureTest` de cada serviço: controller → service → repository/specification/mapper.

Continuam valendo: um banco por serviço, nenhum import entre serviços, ProblemDetail com catálogo, lock otimista e constraints no banco, testes em pirâmide com gates de qualidade.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Manter a hexagonal leve (ADR-004) | Domínio testável sem framework; troca de infraestrutura barata | Mais classes e mapeamentos; padrão estranho ao dono do projeto |
| **Camadas por tipo** | Simples, familiar, menos indireção; padrão amplamente usado em Spring | Regras de negócio acopladas a entidades JPA; pacotes crescem com o número de features |

## Consequências
- **Positivas:** código mais direto, com menos classes e sem mapeamento domínio ↔ JPA; onboarding imediato para quem conhece Spring; testes unitários de service com repository mockado continuam rápidos.
- **Negativas / custos:** a evolução para eventos (F04) e a extração do catálogo (IF-010) dependem de disciplina nos services, não mais de ports; entidades com `@Setter` exigem que as invariantes fiquem no service (e no banco).
- **Resolvido no ADR-026:** os módulos `catalog`, `order` e `payment` do sales-service são pacotes com as camadas por tipo dentro.
- **O que observar:** services crescendo demais (sinal para dividir por caso de uso); regra de negócio vazando para controller ou mapper.
