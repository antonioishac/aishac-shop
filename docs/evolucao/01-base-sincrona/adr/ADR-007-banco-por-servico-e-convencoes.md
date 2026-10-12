# ADR-007 — Um banco por serviço e convenções de modelagem

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D07 · D08 · F01-010 · rules `database` e `flyway` |

## Contexto
Banco compartilhado entre serviços cria acoplamento invisível: um serviço quebra o outro ao mudar uma tabela. Localmente, no entanto, subir um PostgreSQL por serviço pesa na máquina de desenvolvimento.

## Decisão
**Um banco lógico por serviço** (`customers_db`, `sales_db`), sem FK, view ou consulta entre bancos. Localmente roda **um único container PostgreSQL 18** com os dois bancos e **um usuário por serviço** (`customer_app`, `sales_app`), sem acesso cruzado: `CONNECT` revogado de `PUBLIC`. A estrutura muda **somente por migration Flyway**.

Convenções de modelagem:
- chave primária UUID v7;
- dinheiro em `NUMERIC(19,2)`;
- datas em `TIMESTAMPTZ`;
- status em `VARCHAR + CHECK`, sem `ENUM`;
- coluna `version` para lock otimista;
- constraints nomeadas (`pk_`, `fk_`, `uk_`, `ck_`, `idx_`);
- regras invariantes também no banco.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Banco único compartilhado | Simples; joins entre contextos | Acoplamento de esquema; impede deploy independente |
| Um container por banco | Isolamento físico | Mais memória localmente, sem ganho de estudo |
| **Um container, banco e usuário por serviço** | Isolamento lógico garantido por permissão; leve | Isolamento físico só na nuvem (trilha C3) |

## Consequências
- **Positivas:** fronteira de dados verificável (o acesso cruzado falha com `permission denied`); esquema evolui com o serviço.
- **Negativas / custos:** dados de outro serviço só via API; relatórios entre contextos exigem composição (BFF) ou réplica futura.
- **O que observar:** tentações de "só um join" entre bancos; o usuário da aplicação também executa DDL do Flyway, separação prevista no F01-090.
