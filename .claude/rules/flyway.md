---
paths:
  - "services/**/db/migration/**"
  - "services/**/db/seed/**"
---

# Migrations Flyway

- Local: `src/main/resources/db/migration/`. Nome: `V<n>__<descricao_em_snake_case>.sql` (dois underscores), numeração sequencial sem lacunas.
- **Migration já commitada é imutável.** Correção = nova migration. (Um hook bloqueia a edição.)
- Uma intenção por migration (ex.: criar uma tabela e seus índices).
- Dados de exemplo ficam em `src/main/resources/db/seed/` (`R__seed_*.sql` ou versão própria), carregados **apenas no perfil `local`** via `spring.flyway.locations`. Nunca em `dev`/`prod`.
- Cabeçalho de comentário em cada arquivo: item do backlog, objetivo, decisões relevantes.
- Sem `DROP` destrutivo sem item de backlog explícito e aprovação do usuário.
- Toda migration é validada por teste de integração com Testcontainers (o contexto Spring sobe com `ddl-auto=validate`).
- Mudança de estrutura **nunca** pelo MCP do PostgreSQL.
