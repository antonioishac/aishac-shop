-- =============================================================
-- V4__create_product_images
-- Backlog: F01-031   Spec: specs/sales/F01-030-catalog/spec.md (seção 6)
-- Objetivo: imagens de produto (o arquivo fica no S3; o banco guarda a chave).
-- Decisões:
--   - posição única e uma principal por produto, ambas adiáveis (verificadas no COMMIT).
--   - permissões do claude_mcp vêm das default privileges do init (infra/docker/postgres/init).
-- =============================================================

CREATE TABLE product_images (
    id           UUID         NOT NULL DEFAULT uuidv7(),
    product_id   UUID         NOT NULL,
    object_key   VARCHAR(200) NOT NULL,
    content_type VARCHAR(20)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    alt_text     VARCHAR(150) NOT NULL,
    position     SMALLINT     NOT NULL,
    is_primary   BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT uk_product_images_object_key UNIQUE (object_key),
    CONSTRAINT uk_product_images_product_position UNIQUE (product_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uk_product_images_single_primary
        EXCLUDE USING btree (product_id WITH =) WHERE (is_primary) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_product_images_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT ck_product_images_size_positive CHECK (size_bytes > 0),
    CONSTRAINT ck_product_images_alt_text_not_blank CHECK (btrim(alt_text) <> ''),
    CONSTRAINT ck_product_images_position_range CHECK (position BETWEEN 1 AND 8)
);
