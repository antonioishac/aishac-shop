-- =============================================================
-- V2__create_products
-- Backlog: F01-031   Spec: specs/sales/F01-030-catalog/spec.md (seção 6)
-- Objetivo: produtos com SKU, preço, categoria e estoque.
-- Decisões:
--   - stock_quantity só muda por UPDATE atômico (saída com WHERE stock_quantity >= :qty), fora do lock otimista.
--   - permissões do claude_mcp vêm das default privileges do init (infra/docker/postgres/init).
-- =============================================================

CREATE TABLE products (
    id             UUID          NOT NULL DEFAULT uuidv7(),
    sku            VARCHAR(40)   NOT NULL,
    name           VARCHAR(150)  NOT NULL,
    description    VARCHAR(2000),
    price          NUMERIC(19,2) NOT NULL,
    category_id    UUID          NOT NULL,
    stock_quantity INTEGER       NOT NULL DEFAULT 0,
    active         BOOLEAN       NOT NULL DEFAULT true,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_products_sku_format CHECK (sku ~ '^[A-Z0-9-]{3,40}$'),
    CONSTRAINT ck_products_name_length CHECK (name = btrim(name) AND char_length(name) BETWEEN 3 AND 150),
    CONSTRAINT ck_products_price_positive CHECK (price > 0),
    CONSTRAINT ck_products_stock_non_negative CHECK (stock_quantity >= 0)
);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_name_trgm ON products USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_products_price ON products (price);
CREATE INDEX idx_products_created_at ON products (created_at DESC);
