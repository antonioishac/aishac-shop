---
name: gerar-flyway
description: Cria a próxima migration Flyway versionada de um serviço, com numeração correta e cabeçalho padrão. Use somente com o modelo de dados já aprovado.
argument-hint: "[servico] [descricao_em_snake_case]"
arguments: [service, description]
---

# Gerar migration Flyway

Serviço: `$service` · Descrição: `$description`

1. Descubra a próxima versão:
   ```bash
   ${CLAUDE_SKILL_DIR}/scripts/next-version.sh services/$service
   ```
2. Crie `services/$service/src/main/resources/db/migration/V<n>__$description.sql` com o cabeçalho:
   ```sql
   -- =============================================================
   -- V<n>__$description
   -- Backlog: <ID>   Spec: specs/<contexto>/<ID>-<nome>/spec.md
   -- Objetivo: <uma frase>
   -- =============================================================
   ```
3. Escreva o DDL aprovado, seguindo a rule `database` (nomes de constraints, índices em FKs, `CHECK`s).
4. Garanta permissões para o usuário da aplicação e para o `claude_mcp` (somente DML) quando a tabela for nova:
   ```sql
   GRANT SELECT, INSERT, UPDATE, DELETE ON <tabela> TO <servico>_app;
   GRANT SELECT, INSERT, UPDATE, DELETE ON <tabela> TO claude_mcp;
   ```
   > Se as permissões padrão (`ALTER DEFAULT PRIVILEGES`) já estiverem configuradas no script de inicialização, este passo é desnecessário — verifique `infra/docker/postgres/init/`.
5. Rode `./mvnw verify` no serviço para aplicar a migration do zero via Testcontainers.

Nunca edite uma migration já commitada.
