---
name: modelar-dados
description: Projeta o modelo de dados PostgreSQL de uma spec aprovada (tabelas, colunas, constraints, índices e DDL proposto) e o registra na seção "Modelo de dados" da spec para aprovação. Use antes de criar qualquer migration.
argument-hint: "[caminho da spec.md]"
---

# Modelar dados

Siga a rule `database`. Entrada: a `spec.md` aprovada ($ARGUMENTS).

## Passos
1. Leia as migrations existentes do serviço para conhecer o esquema atual.
2. Derive entidades das regras de negócio e dos critérios de aceite. Para cada tabela, documente na spec:

   | Coluna | Tipo | Nulo | Default | Regra/constraint | Origem (RN/CA) |
   |---|---|---|---|---|---|

3. Liste constraints (PK, FK, UNIQUE, CHECK, índices únicos parciais) com nome conforme convenção.
4. Liste índices e **a consulta que cada um atende** (filtro, ordenação ou FK).
5. Marque referências lógicas a outro serviço/módulo (sem FK) e explique.
6. Descreva o comportamento de concorrência (lock otimista, atualização atômica, ordem de travamento).
7. Escreva o **DDL proposto** em bloco SQL (ainda não é a migration).
8. Inclua um mini-ER em texto:
   ```text
   orders 1──N order_items
   orders ┄┄ customer (lógico, outro serviço)
   ```
9. Pare e peça aprovação do usuário (Portão 2).

Depois de aprovado, crie a migration com a skill `gerar-flyway`.
