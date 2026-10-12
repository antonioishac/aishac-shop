# ADR-017 — Idempotência na criação de pedido com Idempotency-Key

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D20 · F01-042 · F01-049 · F01-064 |

## Contexto
Em chamadas síncronas, timeout e retry (ADR-009) são normais: o cliente não sabe se o pedido foi criado e reenvia. Sem proteção, um duplo clique ou um retry geram **dois pedidos e duas baixas de estoque**.

## Decisão
`POST` de criação de pedido **exige o cabeçalho `Idempotency-Key`** (UUID gerado pelo frontend por tentativa de compra e reaproveitado nos reenvios). A garantia **nasce no banco**: uma restrição única na chave, por cliente, faz a segunda inserção concorrente falhar. A requisição repetida devolve o **mesmo pedido** já criado. Se a mesma chave chegar com um corpo diferente, a resposta é erro. O teste de concorrência com Virtual Threads (F01-049) dispara a mesma chave em paralelo e espera exatamente um pedido.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Desabilitar o botão no frontend | Trivial | Não cobre retry de rede, BFF ou Resilience4j |
| Checar "pedido parecido recente" | Sem cabeçalho | Heurística; falso positivo em compras legítimas repetidas |
| Cache de chaves em memória | Rápido | Perde-se no restart; não funciona com várias instâncias |
| **Chave + restrição única no banco** | Correto sob concorrência e com várias instâncias | Cabeçalho obrigatório no contrato; armazenar chaves |

## Consequências
- **Positivas:** retry seguro de ponta a ponta (frontend, BFF e serviço).
- **Negativas / custos:** contrato mais rígido; política de expiração das chaves a definir na spec.
- **O que observar:** pedidos duplicados em produção (indicaria chave gerada por requisição, e não por tentativa).
