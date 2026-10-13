# ADR-016 — Cliente com histórico é desativado, nunca excluído

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D18 · F01-020 · F01-022 · IF-005 |

## Contexto
Pedidos referenciam o cliente por UUID, sem FK entre bancos (ADR-007). Excluir um cliente deixaria pedidos apontando para um ID inexistente e apagaria dados que a LGPD e as obrigações fiscais podem exigir manter.

## Decisão
Não existe exclusão física de cliente. O ciclo de vida é por status: **`ACTIVE`**, **`BLOCKED`** (bloqueio administrativo) e **`INACTIVE`** (desativado). Transições permitidas: `ACTIVE` ↔ `BLOCKED` e `ACTIVE`/`BLOCKED` → `INACTIVE`; na fase 01, `INACTIVE` é final. Clientes `BLOCKED` ou `INACTIVE` não criam pedidos; o sales-service verifica o status pela API do customer-service. O pedido guarda um *snapshot* dos dados de entrega, para não depender do cadastro atual.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Exclusão física | Simples; "direito ao esquecimento" literal | Quebra a referência dos pedidos; perde histórico |
| *Soft delete* genérico (`deleted_at`) | Reversível | Filtros esquecidos vazam registros; não distingue bloqueio de desativação |
| **Status explícito no domínio** | Regra de negócio clara; histórico preservado | Atender à LGPD exige anonimização (ainda não implementada) |

## Consequências
- **Positivas:** pedidos sempre íntegros; transições de status testáveis sem Spring (`CustomerRules`, na camada service, ADR-025) e reforçadas por CHECK no banco.
- **Negativas / custos:** dados pessoais permanecem até existir a anonimização (IF-005).
- **O que observar:** pedidos de exclusão por titulares de dados, que motivam a priorização do IF-005.
