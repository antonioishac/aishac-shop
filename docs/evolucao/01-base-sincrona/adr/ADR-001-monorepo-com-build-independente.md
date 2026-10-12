# ADR-001 — Monorepo com build independente por serviço

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D01 · F01-001 |

## Contexto
O projeto tem vários serviços Java, um frontend, configuração, infraestrutura e documentação que evoluem juntos durante o estudo. Ao mesmo tempo, cada serviço precisa poder ser construído, testado, versionado e implantado sozinho, como numa organização com times por serviço.

## Decisão
Um único repositório Git, com **um projeto Maven independente por serviço**: cada um tem seu Maven Wrapper e um `pom.xml` filho direto do `spring-boot-starter-parent`. Não há POM agregador nem **biblioteca compartilhada** de domínio, DTOs ou utilitários. O código transversal (filtro de correlação, handler de erros, máscara de logs) é **duplicado de propósito** em cada serviço.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Um repositório por serviço | Isolamento total; permissões por repositório | Mudanças transversais exigem vários PRs; estudo fragmentado |
| Monorepo com POM agregador e módulo `common` | Menos duplicação; build único | Acoplamento por classe entre serviços; deploy e versão deixam de ser independentes |
| **Monorepo com builds independentes** | Visão única do sistema; serviços desacoplados; CI por caminho | Duplicação do código transversal; versões de dependências alinhadas à mão |

## Consequências
- **Positivas:** serviços só se conhecem por contrato HTTP; CI e Dependabot por pasta; extrair um serviço para outro repositório é trivial.
- **Negativas / custos:** a infraestrutura transversal existe em cópias que podem divergir; atualizar versões exige tocar cada `pom.xml` (mitigado pelo Dependabot).
- **O que observar:** divergência entre as cópias do código transversal. Se crescer, avaliar um *starter* interno versionado, sem domínio.
