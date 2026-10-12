# Ambiente local (Docker Compose)

> **Você executa todos os comandos desta página.** O Claude só escreve os arquivos (premissa P1).
> Rode tudo a partir de `infra/docker/`.

| Arquivo | Conteúdo |
|---|---|
| `compose.infra.yml` | PostgreSQL 18 (`aishac_customer`, `aishac_sales`) e LocalStack (S3) |
| `compose.apps.yml` | config-server, customer-service, sales-service, web-bff, web. **Inclui** o `compose.infra.yml` |
| `postgres/init/` | Bancos, usuários por serviço e `claude_mcp` (roda só na primeira criação do volume) |
| `localstack/init/` | Cria o bucket privado de imagens |
| `.env.example` | Variáveis necessárias, sem valores |

## 1. Preparar o `.env`

**Atalho (Windows/PowerShell):** gera o `.env` com senhas aleatórias e define as variáveis do MCP (seção 6), sem exibir nenhuma senha:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-local-env.ps1                    # porta 5432
powershell -ExecutionPolicy Bypass -File .\setup-local-env.ps1 -PostgresPort 5433 # se a 5432 já estiver ocupada
```

Depois, preencha só o `LOCALSTACK_AUTH_TOKEN` no `.env` (F01-018). Sem ele, o postgres sobe normalmente e o LocalStack não inicia.

**Manual:**

```bash
cp .env.example .env
# edite .env: senhas dos três usuários do banco, do superusuário e o LOCALSTACK_AUTH_TOKEN
```

> **Porta diferente de 5432?** Os serviços rodando fora do Docker (IDE, `./mvnw spring-boot:run`) precisam de `DB_PORT` com a mesma porta. Dentro do compose nada muda: os containers falam com `postgres:5432` pela rede interna.

## 2. Infraestrutura (PostgreSQL + LocalStack)

```bash
docker compose -f compose.infra.yml up -d          # subir
docker compose -f compose.infra.yml ps             # estado e healthcheck
docker compose -f compose.infra.yml logs -f        # logs
docker compose -f compose.infra.yml down           # derrubar (mantém os dados)
docker compose -f compose.infra.yml down -v        # derrubar e APAGAR o volume (reaplica os scripts de init)
```

## 3. Tudo (infra + aplicações)

Disponível depois que os serviços tiverem Dockerfile (F01-012) e o frontend existir (F01-060).

```bash
docker compose -f compose.apps.yml up -d --build
docker compose -f compose.apps.yml logs -f sales-service
docker compose -f compose.apps.yml stop customer-service   # estudo F01-050: derrubar Clientes
docker compose -f compose.apps.yml down
```

| Serviço | URL local |
|---|---|
| web | http://localhost:3000 |
| web-bff | http://localhost:8080 |
| customer-service | http://localhost:8081 |
| sales-service | http://localhost:8082 |
| config-server | http://localhost:8888 |
| PostgreSQL | localhost:5432 |
| LocalStack | http://localhost:4566 |

As portas ficam presas a `127.0.0.1`: nada fica exposto na rede.

## 4. Validar os bancos (F01-019)

Cada usuário só acessa o próprio banco:

```bash
# deve funcionar
docker compose -f compose.infra.yml exec postgres psql -U customer_app -d aishac_customer -c "select current_user"
docker compose -f compose.infra.yml exec postgres psql -U sales_app    -d aishac_sales     -c "select current_user"

# deve falhar com "permission denied for database"
docker compose -f compose.infra.yml exec postgres psql -U customer_app -d aishac_sales -c "select 1"
docker compose -f compose.infra.yml exec postgres psql -U sales_app    -d aishac_customer -c "select 1"

# claude_mcp: conecta nos dois, mas não cria tabela (deve falhar com "permission denied for schema public")
docker compose -f compose.infra.yml exec postgres psql -U claude_mcp -d aishac_sales -c "create table t(id int)"
```

Dentro do container a autenticação local é `trust`, então não pede senha. Para testar com senha, conecte pela porta publicada com um cliente no host.

Bucket no LocalStack:

```bash
docker compose -f compose.infra.yml exec localstack awslocal s3 ls
```

## 5. Auditar o que o Claude executa no banco (D21)

O `claude_mcp` tem `log_statement = 'all'` e o log mostra `usuário@banco` em cada linha:

```bash
docker compose -f compose.infra.yml logs -f postgres | grep claude_mcp
```

## 6. MCP do PostgreSQL

Defina no **seu** ambiente (fora do repositório), com a senha que você colocou em `CLAUDE_MCP_DB_PASSWORD`:

```bash
export CLAUDE_PG_CUSTOMERS_URL="postgresql://claude_mcp:<senha>@localhost:5432/aishac_customer"
export CLAUDE_PG_SALES_URL="postgresql://claude_mcp:<senha>@localhost:5432/aishac_sales"
```

Depois, reinicie o Claude Code para os servidores MCP conectarem.

## Observações

- **Senhas só valem na criação do volume.** Mudou o `.env` depois? Use `down -v` (apaga os dados) e suba de novo.
- **Windows:** os scripts `.sh` precisam de quebra de linha LF (garantido pelo `.gitattributes` depois do `git init`). Com CRLF, o init do PostgreSQL falha com `bad interpreter`.
- **Versões fixadas:** `postgres:18.4-alpine` e `localstack/localstack:2026.03.0`. Existem versões mais novas; atualize conscientemente, conferindo a tag no Docker Hub.
- O `claude_mcp` recebe privilégios de DML por *default privileges* nas tabelas criadas pelo dono de cada banco; isso inclui a `flyway_schema_history`. Não use o MCP para alterá-la.
