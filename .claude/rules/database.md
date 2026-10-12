---
paths:
  - "services/**/db/migration/**"
  - "services/**/adapter/out/persistence/**"
  - "infra/docker/postgres/**"
---

# Banco de dados (PostgreSQL)

## Isolamento
- Um banco por serviço: `customers_db` (usuário `customer_app`), `sales_db` (usuário `sales_app`).
- **Nenhuma FK, view ou consulta entre bancos.** Referência a outro serviço é só um UUID, sem FK física.
- Dentro do `sales-service`, referências entre módulos (`order` → `catalog`) também são lógicas, sem FK, para permitir a extração futura do catálogo.

## Convenções de nomes
- Tabelas em `snake_case`, plural, inglês: `customer_addresses`.
- Constraints sempre nomeadas: `pk_<tabela>`, `fk_<tabela>_<ref>`, `uk_<tabela>_<colunas>`, `ck_<tabela>_<regra>`, `idx_<tabela>_<colunas>`.

## Tipos
| Dado | Tipo |
|---|---|
| Chave primária | `UUID` (v7, ordenável no tempo) |
| Dinheiro | `NUMERIC(19,2)` |
| Data/hora | `TIMESTAMPTZ` |
| Status | `VARCHAR(20)` + `CHECK (status IN (...))` (não usar `ENUM` do PostgreSQL) |
| Texto livre limitado | `VARCHAR(n)` com tamanho justificado |
| Dados semiestruturados | `JSONB` (somente quando justificado em ADR) |

## Colunas padrão de agregado
`version BIGINT NOT NULL DEFAULT 0` (lock otimista, `@Version`), `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`.

## Integridade e desempenho
- Regras invariantes também no banco (`CHECK`, `UNIQUE`, índices únicos parciais), não só na aplicação.
- **Toda FK tem índice.** Índices para filtros e ordenações previstos na spec.
- Concorrência de estoque: `UPDATE ... SET stock_quantity = stock_quantity - :qty WHERE id = :id AND stock_quantity >= :qty`; verificar linhas afetadas. Múltiplos produtos: processar em **ordem crescente de ID**.

## JPA
- Entidades em `adapter/out/persistence`, separadas do domínio; conversão via MapStruct.
- `FetchType.LAZY` em todas as associações; consultas de listagem com projeções ou `JOIN FETCH` explícito. Proibido N+1.
- `spring.jpa.open-in-view=false`; `ddl-auto=validate`.
