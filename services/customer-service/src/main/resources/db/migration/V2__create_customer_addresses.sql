-- =============================================================
-- V2__create_customer_addresses
-- Backlog: F01-021   Spec: specs/customer/F01-020-customer-registration/spec.md (seção 6)
-- Objetivo: endereços de entrega do cliente (parte do agregado Customer).
-- Decisões:
--   - um padrão por cliente via EXCLUDE adiável (verificado no COMMIT): trocar o padrão
--     funciona em qualquer ordem de gravação; índice único parcial seria checado linha a linha.
--   - sem coluna version: o lock otimista é a version de customers (raiz do agregado).
--   - limite de 10 endereços garantido pelo agregado, não por CHECK.
-- =============================================================

CREATE TABLE customer_addresses (
    id          UUID         NOT NULL DEFAULT uuidv7(),
    customer_id UUID         NOT NULL,
    label       VARCHAR(40)  NOT NULL,
    zip_code    CHAR(8)      NOT NULL,
    street      VARCHAR(150) NOT NULL,
    number      VARCHAR(10)  NOT NULL,
    complement  VARCHAR(60),
    district    VARCHAR(80)  NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    state       CHAR(2)      NOT NULL,
    country     CHAR(2)      NOT NULL DEFAULT 'BR',
    is_default  BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT uk_customer_addresses_single_default
        EXCLUDE USING btree (customer_id WITH =) WHERE (is_default)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_customer_addresses_label_not_blank CHECK (btrim(label) <> ''),
    CONSTRAINT ck_customer_addresses_zip_code_format CHECK (zip_code ~ '^[0-9]{8}$'),
    CONSTRAINT ck_customer_addresses_state CHECK (state IN (
        'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA',
        'PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    CONSTRAINT ck_customer_addresses_country CHECK (country = 'BR')
);

-- Índice da FK; lista os endereços do cliente; acha o mais recente (RN-15); conta endereços (RN-12).
CREATE INDEX idx_customer_addresses_customer_created ON customer_addresses (customer_id, created_at DESC);
