-- =============================================================
-- V3__create_stock_movements
-- Backlog: F01-031   Spec: specs/sales/F01-030-catalog/spec.md (seção 6)
-- Objetivo: histórico append-only de movimentações de estoque.
-- Decisões:
--   - tipos ENTRY/EXIT; os pedidos (F01-041) acrescentarão tipos por migration nova.
--   - permissões do claude_mcp vêm das default privileges do init (infra/docker/postgres/init).
-- =============================================================

CREATE TABLE stock_movements (
    id              UUID         NOT NULL DEFAULT uuidv7(),
    product_id      UUID         NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    resulting_stock INTEGER      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT ck_stock_movements_type CHECK (type IN ('ENTRY', 'EXIT')),
    CONSTRAINT ck_stock_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_stock_movements_reason_length CHECK (char_length(btrim(reason)) BETWEEN 3 AND 255),
    CONSTRAINT ck_stock_movements_resulting_non_negative CHECK (resulting_stock >= 0)
);
CREATE INDEX idx_stock_movements_product_created ON stock_movements (product_id, created_at DESC, id DESC);
