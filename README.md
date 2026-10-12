# Aishac Shop

E-commerce de estudo, de ponta a ponta: microserviços Java 25 / Spring Boot 4, BFF, frontend React + TypeScript, PostgreSQL, S3 e, nas fases seguintes, API Gateway, autenticação, Kafka e AWS (Terraform, EKS).

- **Backlog e decisões:** [docs/backlog/README.md](docs/backlog/README.md)
- **Arquitetura por fase:** [docs/evolucao/](docs/evolucao/)
- **Instruções para o Claude Code:** [CLAUDE.md](CLAUDE.md)

## Pré-requisitos locais

- Java 25, Node.js LTS, Docker (você controla os containers)
- `bash` e `jq` (usados pelos hooks do Claude Code; no Windows, use WSL)
- `uv` (executa o servidor MCP do PostgreSQL via `uvx`)
- Conta LocalStack (plano Hobby) com `LOCALSTACK_AUTH_TOKEN`

## Claude Code

| Recurso | Onde |
|---|---|
| Instruções gerais | `CLAUDE.md` (raiz e por serviço) |
| Regras por tipo de arquivo | `.claude/rules/` |
| Subagentes | `.claude/agents/` |
| Skills (`/nome`) | `.claude/skills/` |
| Permissões e hooks | `.claude/settings.json`, `.claude/hooks/` |
| MCP do PostgreSQL | `.mcp.json` |

### Variáveis de ambiente para o MCP do PostgreSQL

Defina no seu ambiente (nunca no repositório), apontando para o **seu container local** com o usuário `claude_mcp`:

```bash
export CLAUDE_PG_CUSTOMERS_URL="postgresql://claude_mcp:<senha>@localhost:5432/customers_db"
export CLAUDE_PG_SALES_URL="postgresql://claude_mcp:<senha>@localhost:5432/sales_db"
```

Para acompanhar tudo o que o Claude executar no banco, siga o log do container PostgreSQL (comando no `infra/docker/README.md`).
