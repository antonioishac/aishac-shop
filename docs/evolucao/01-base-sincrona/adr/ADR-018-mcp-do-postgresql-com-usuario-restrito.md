# ADR-018 — MCP do PostgreSQL com usuário restrito e auditado

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D21 · F01-007 · F01-010 · premissas P1 e P2 |

## Contexto
O Claude Code consegue consultar e preparar dados de desenvolvimento com um servidor MCP do PostgreSQL, o que acelera testes manuais e investigação. Um agente com acesso irrestrito ao banco, porém, poderia alterar a estrutura fora das migrations ou ler dados de outro serviço sem que o usuário perceba.

## Decisão
- Servidores MCP apontando **só para o container local**, com URL vinda do ambiente do usuário (`CLAUDE_PG_CUSTOMERS_URL`, `CLAUDE_PG_SALES_URL`), nunca do repositório.
- Usuário **`claude_mcp`**: `CONNECT` nos dois bancos, `USAGE` no schema e `SELECT/INSERT/UPDATE/DELETE` por *default privileges* nas tabelas dos donos. **Sem `CREATE`**, portanto sem DDL; estrutura só por Flyway.
- **Auditoria:** `log_statement = 'all'` e `statement_timeout = 30s` para o `claude_mcp`; o log mostra `usuário@banco` em cada linha.
- Servidores de leitura liberados; os de escrita **pedem aprovação** a cada uso (`settings.json`).

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Sem MCP | Nenhum risco | O agente não consegue preparar nem verificar dados |
| MCP com o usuário da aplicação | Simples | Permite DDL; acesso a todas as operações sem trilha separada |
| **Usuário dedicado, sem DDL, auditado** | Menor privilégio; tudo rastreável | Mais configuração inicial |

## Consequências
- **Positivas:** o usuário vê no log tudo o que o agente executou; mudanças de estrutura continuam passando por migration e revisão.
- **Negativas / custos:** as *default privileges* também dão escrita na `flyway_schema_history` (não usar o MCP para alterá-la).
- **O que observar:** comandos inesperados no log do `claude_mcp`; necessidade de restringir a escrita a tabelas específicas.
