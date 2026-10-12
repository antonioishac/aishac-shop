# Git e commits

- **Conventional Commits**: `<tipo>(<escopo>): <descrição no imperativo, em inglês>`.
  - Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`.
  - Escopos: `customer`, `sales`, `catalog`, `order`, `payment`, `bff`, `web`, `config`, `infra`, `claude`, `docs`.
  - Rodapé com o item do backlog: `Refs: F01-042`.
- Um commit = uma mudança coerente que compila e passa nos testes.
- Branches: `main` protegida; trabalho em `feat/F01-042-create-order`, `fix/...`, `docs/...`. Merge via PR com o template.
- **Proibido:** `git push --force`, reescrever histórico de `main`, commitar `.env` ou qualquer segredo (um hook verifica).
- Versionamento semântico **por serviço**; CHANGELOG atualizado a cada release.
- Tag de fechamento de fase: `fase-XX-<nome>` (criada pelo usuário).
