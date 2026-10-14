-- =============================================================
-- V1__create_categories
-- Backlog: F01-031   Spec: specs/sales/F01-030-catalog/spec.md (seção 6)
-- Objetivo: categorias de produto (um nível), com nome único sem diferenciar maiúsculas.
-- Decisões:
--   - pg_trgm (extensão confiável) é criada aqui; usada pela busca "nome contém" de produtos (V2).
--   - permissões do claude_mcp vêm das default privileges do init (infra/docker/postgres/init).
-- =============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE categories (
    id          UUID         NOT NULL DEFAULT uuidv7(),
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT true,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT ck_categories_name_length
        CHECK (name = btrim(name) AND char_length(name) BETWEEN 2 AND 80)
);
CREATE UNIQUE INDEX uk_categories_name_lower ON categories (lower(name));
CREATE INDEX idx_categories_active_name ON categories (active, name);
