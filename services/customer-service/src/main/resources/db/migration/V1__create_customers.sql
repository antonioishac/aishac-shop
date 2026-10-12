-- =============================================================
-- V1__create_customers
-- Backlog: F01-021   Spec: specs/customer/F01-020-customer-registration/spec.md (seção 6)
-- Objetivo: tabela de clientes (agregado raiz) e índices de listagem e busca.
-- Decisões:
--   - id gerado pela aplicação (UUID v7); uuidv7() só como default para inserts manuais.
--   - e-mail guardado normalizado (CHECK), então a UNIQUE ignora maiúsculas.
--   - pg_trgm (extensão confiável) para a busca "nome contém".
--   - permissões do claude_mcp vêm das default privileges do init (infra/docker/postgres/init).
-- =============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE customers (
    id             UUID         NOT NULL DEFAULT uuidv7(),
    full_name      VARCHAR(150) NOT NULL,
    cpf            CHAR(11)     NOT NULL,
    email          VARCHAR(254) NOT NULL,
    phone          VARCHAR(11),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    block_reason   VARCHAR(255),
    blocked_at     TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_cpf UNIQUE (cpf),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT ck_customers_full_name_length
        CHECK (full_name = btrim(full_name) AND char_length(full_name) BETWEEN 3 AND 150),
    CONSTRAINT ck_customers_cpf_format CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_customers_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_customers_phone_format CHECK (phone ~ '^[0-9]{10,11}$'),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE')),
    CONSTRAINT ck_customers_block_data CHECK (
        (status = 'BLOCKED') = (block_reason IS NOT NULL AND blocked_at IS NOT NULL)
        AND (block_reason IS NULL) = (blocked_at IS NULL)),
    CONSTRAINT ck_customers_deactivation_data CHECK ((status = 'INACTIVE') = (deactivated_at IS NOT NULL))
);

-- Listagem sem filtro, ordenação padrão createdAt,desc.
CREATE INDEX idx_customers_created_at ON customers (created_at DESC);
-- Listagem filtrada por status com a ordenação padrão.
CREATE INDEX idx_customers_status_created_at ON customers (status, created_at DESC);
-- Filtro "nome contém", sem diferenciar maiúsculas.
CREATE INDEX idx_customers_full_name_trgm ON customers USING gin (lower(full_name) gin_trgm_ops);
