-- =============================================================
-- R__seed_catalog (repeatable)
-- Backlog: F01-031   Spec: specs/sales/F01-030-catalog/spec.md (seção 6, "Dados de exemplo")
-- Objetivo: categorias e produtos de exemplo para o ambiente local.
-- Decisões:
--   - carregado SÓ no perfil local (spring.flyway.locations em config-repo/sales-service-local.properties).
--   - ids fixos + ON CONFLICT DO NOTHING: reexecutar não duplica nem sobrescreve dados alterados no uso.
--   - sem imagens: o seed não envia arquivos ao S3.
-- =============================================================

INSERT INTO categories (id, name, description, active, created_at, updated_at) VALUES
    ('01900000-0000-7000-8000-000000000001', 'Calçados', 'Tênis, sapatos e sandálias', true, now(), now()),
    ('01900000-0000-7000-8000-000000000002', 'Roupas', 'Camisetas, calças e agasalhos', true, now(), now()),
    ('01900000-0000-7000-8000-000000000003', 'Acessórios', 'Bonés, mochilas e garrafas', true, now(), now()),
    ('01900000-0000-7000-8000-000000000004', 'Eletrônicos', 'Relógios e fones esportivos', true, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO products (id, sku, name, description, price, category_id, stock_quantity, active, created_at, updated_at) VALUES
    ('01900000-0000-7000-8000-000000000101', 'TENIS-RUN-42', 'Tênis Run', 'Tênis de corrida leve', 299.90, '01900000-0000-7000-8000-000000000001', 25, true, now(), now()),
    ('01900000-0000-7000-8000-000000000102', 'TENIS-TRAIL-41', 'Tênis Trail', 'Tênis para trilha com solado reforçado', 459.90, '01900000-0000-7000-8000-000000000001', 12, true, now(), now()),
    ('01900000-0000-7000-8000-000000000103', 'SANDALIA-CONF-39', 'Sandália Conforto', 'Sandália com palmilha anatômica', 89.90, '01900000-0000-7000-8000-000000000001', 40, true, now(), now()),
    ('01900000-0000-7000-8000-000000000104', 'SAPATO-SOCIAL-42', 'Sapato Social', 'Sapato em couro', 349.00, '01900000-0000-7000-8000-000000000001', 0, true, now(), now()),
    ('01900000-0000-7000-8000-000000000105', 'CHINELO-BASIC', 'Chinelo Básico', 'Chinelo de borracha', 29.90, '01900000-0000-7000-8000-000000000001', 100, false, now(), now()),
    ('01900000-0000-7000-8000-000000000201', 'CAMISETA-DRY-M', 'Camiseta Dry', 'Camiseta esportiva de secagem rápida', 79.90, '01900000-0000-7000-8000-000000000002', 60, true, now(), now()),
    ('01900000-0000-7000-8000-000000000202', 'CAMISETA-ALGODAO-G', 'Camiseta Algodão', 'Camiseta 100% algodão', 59.90, '01900000-0000-7000-8000-000000000002', 80, true, now(), now()),
    ('01900000-0000-7000-8000-000000000203', 'CALCA-MOLETOM-M', 'Calça Moletom', 'Calça de moletom com punho', 149.90, '01900000-0000-7000-8000-000000000002', 30, true, now(), now()),
    ('01900000-0000-7000-8000-000000000204', 'JAQUETA-CORTA-VENTO', 'Jaqueta Corta-Vento', 'Jaqueta leve e impermeável', 229.90, '01900000-0000-7000-8000-000000000002', 15, true, now(), now()),
    ('01900000-0000-7000-8000-000000000205', 'SHORT-CORRIDA-P', 'Short de Corrida', 'Short com bolso interno', 69.90, '01900000-0000-7000-8000-000000000002', 45, true, now(), now()),
    ('01900000-0000-7000-8000-000000000301', 'BONE-ABA-CURVA', 'Boné Aba Curva', 'Boné com ajuste traseiro', 49.90, '01900000-0000-7000-8000-000000000003', 70, true, now(), now()),
    ('01900000-0000-7000-8000-000000000302', 'MOCHILA-25L', 'Mochila 25 L', 'Mochila com compartimento para notebook', 199.90, '01900000-0000-7000-8000-000000000003', 20, true, now(), now()),
    ('01900000-0000-7000-8000-000000000303', 'GARRAFA-TERMICA-1L', 'Garrafa Térmica 1 L', 'Mantém a temperatura por 12 horas', 119.90, '01900000-0000-7000-8000-000000000003', 35, true, now(), now()),
    ('01900000-0000-7000-8000-000000000304', 'MEIA-KIT-3', 'Kit 3 Meias', 'Meias esportivas cano médio', 39.90, '01900000-0000-7000-8000-000000000003', 120, true, now(), now()),
    ('01900000-0000-7000-8000-000000000305', 'POCHETE-RUN', 'Pochete de Corrida', 'Pochete elástica para celular', 59.90, '01900000-0000-7000-8000-000000000003', 1, true, now(), now()),
    ('01900000-0000-7000-8000-000000000401', 'RELOGIO-GPS', 'Relógio GPS', 'Relógio esportivo com GPS', 1299.00, '01900000-0000-7000-8000-000000000004', 8, true, now(), now()),
    ('01900000-0000-7000-8000-000000000402', 'FONE-BT-SPORT', 'Fone Bluetooth Sport', 'Fone sem fio resistente a suor', 249.90, '01900000-0000-7000-8000-000000000004', 18, true, now(), now()),
    ('01900000-0000-7000-8000-000000000403', 'CINTA-CARDIACA', 'Cinta Cardíaca', 'Sensor de frequência cardíaca', 189.90, '01900000-0000-7000-8000-000000000004', 10, true, now(), now()),
    ('01900000-0000-7000-8000-000000000404', 'BRACADEIRA-CELULAR', 'Braçadeira para Celular', 'Braçadeira ajustável', 49.90, '01900000-0000-7000-8000-000000000004', 50, true, now(), now()),
    ('01900000-0000-7000-8000-000000000405', 'BALANCA-SMART', 'Balança Smart', 'Balança com bioimpedância', 279.90, '01900000-0000-7000-8000-000000000004', 6, true, now(), now())
ON CONFLICT DO NOTHING;

-- Estoque inicial no histórico (RN-15): uma entrada por produto do seed.
INSERT INTO stock_movements (id, product_id, type, quantity, reason, resulting_stock, created_at)
SELECT uuidv7(), p.id, 'ENTRY', p.stock_quantity, 'Estoque inicial (seed)', p.stock_quantity, now()
FROM products p
WHERE p.id::text LIKE '01900000-0000-7000-8000-%'
  AND p.stock_quantity > 0
  AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.product_id = p.id);
