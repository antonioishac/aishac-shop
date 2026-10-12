# Fluxo de contribuição

Regras de commit também em `.claude/rules/git-commits.md`; processo de feature em `CLAUDE.md` (SDD).

## Branches

- `main` é protegida: só recebe merge via PR com CI verde.
- Uma branch por item do backlog, criada a partir da `main` (ou da branch do item de que depende, se ainda não houve merge):

| Prefixo | Uso | Exemplo |
|---|---|---|
| `feat/` | Funcionalidade (FEAT) ou base técnica nova | `feat/F01-042-create-order` |
| `fix/` | Correção | `fix/F01-042-duplicate-order-on-retry` |
| `docs/` | Documentação, ADRs, diagrama | `docs/F01-008-adrs` |
| `chore/`, `build/`, `ci/` | Ferramentas, build, pipeline | `chore/F01-015-dev-experience` |

- Branches empilhadas são mescladas **na ordem**; abra o PR seguinte depois do merge do anterior.
- Proibido `git push --force` e reescrever o histórico da `main`.

## Commits

Conventional Commits em inglês, no imperativo, com escopo e rodapé do backlog:

```text
feat(sales): reserve stock in ascending product id order

Refs: F01-042
```

Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`.
Escopos: `customer`, `sales`, `catalog`, `order`, `payment`, `bff`, `web`, `config`, `infra`, `claude`, `docs`.
Cada commit compila e passa nos testes.

## Pull requests

- Use o template (`.github/pull_request_template.md`): ele traz a Definition of Done como checklist.
- Um PR por item do backlog. Revisão do `code-reviewer` antes do merge.
- A CI roda só os workflows dos caminhos alterados, mais as varreduras de segurança.

## Versionamento e release (por serviço)

Cada serviço tem **versão semântica própria** no `pom.xml` (`MAJOR.MINOR.PATCH`), independente dos demais (ADR-001).

| Mudança | Incremento |
|---|---|
| Quebra de contrato público (endpoint, payload, comportamento) | MAJOR (ou nova versão de API, ADR-022) |
| Funcionalidade nova compatível | MINOR |
| Correção compatível | PATCH |

Durante o desenvolvimento, a versão fica em `-SNAPSHOT` e o `CHANGELOG.md` do serviço acumula as mudanças em `[Unreleased]`, no formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

Release de um serviço (feita pelo usuário):

1. Mover `[Unreleased]` para `[X.Y.Z] - AAAA-MM-DD` no `CHANGELOG.md` do serviço.
2. Tirar o `-SNAPSHOT` do `pom.xml`, commitar (`chore(<escopo>): release X.Y.Z`) e criar a tag `<servico>/vX.Y.Z` (ex.: `customer-service/v0.1.0`).
3. Voltar o `pom.xml` para a próxima versão `-SNAPSHOT`.

Fechamento de fase: tag `fase-XX-<nome>` (ex.: `fase-01-base-sincrona`), criada pelo usuário.
