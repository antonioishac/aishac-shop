#!/usr/bin/env bash
# Executado pelo entrypoint do PostgreSQL apenas na primeira inicialização do volume.
# Cria um banco por serviço (D07), um usuário por serviço sem acesso cruzado
# e o usuário claude_mcp (dados sim, estrutura não — D21).
# Senhas vêm do ambiente (.env do usuário); nada é versionado.
set -euo pipefail

: "${CUSTOMER_DB_PASSWORD:?}"
: "${SALES_DB_PASSWORD:?}"
: "${CLAUDE_MCP_DB_PASSWORD:?}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v customer_pw="$CUSTOMER_DB_PASSWORD" \
  -v sales_pw="$SALES_DB_PASSWORD" \
  -v mcp_pw="$CLAUDE_MCP_DB_PASSWORD" <<'EOSQL'
-- Usuários
CREATE ROLE customer_app LOGIN PASSWORD :'customer_pw';
CREATE ROLE sales_app    LOGIN PASSWORD :'sales_pw';
CREATE ROLE claude_mcp   LOGIN PASSWORD :'mcp_pw';

-- Auditoria: tudo o que o claude_mcp executar aparece no log do container.
ALTER ROLE claude_mcp SET log_statement = 'all';
ALTER ROLE claude_mcp SET statement_timeout = '30s';

-- Bancos: o usuário do serviço é o dono (executa o Flyway; separação DDL/DML é o item F01-090).
CREATE DATABASE customers_db OWNER customer_app;
CREATE DATABASE sales_db     OWNER sales_app;

-- Sem acesso público: cada usuário só conecta no que lhe foi concedido.
REVOKE ALL ON DATABASE customers_db FROM PUBLIC;
REVOKE ALL ON DATABASE sales_db     FROM PUBLIC;
REVOKE ALL ON DATABASE postgres     FROM PUBLIC;

GRANT CONNECT ON DATABASE customers_db TO claude_mcp;
GRANT CONNECT ON DATABASE sales_db     TO claude_mcp;
EOSQL

# claude_mcp: lê e altera dados das tabelas criadas pelo dono do banco, sem permissão de DDL.
grant_mcp() {
  local db="$1" owner="$2"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" <<EOSQL
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO claude_mcp;
ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO claude_mcp;
ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO claude_mcp;
EOSQL
}

grant_mcp customers_db customer_app
grant_mcp sales_db sales_app
