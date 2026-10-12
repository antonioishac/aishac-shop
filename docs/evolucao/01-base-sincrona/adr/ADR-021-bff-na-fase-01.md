# ADR-021 — BFF (`web-bff`) já na fase 01

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D26 · D24 · F01-051 (detalha a divisão com o Gateway) · rule `bff` |

## Contexto
As telas precisam de dados de mais de um serviço (ex.: pedido com dados do cliente). Se o frontend chamar os serviços diretamente, ele conhece a topologia interna, faz várias chamadas por tela e quebra a cada mudança de contrato interno. O API Gateway, que resolveria parte disso na borda, só chega na F02 (ADR-006).

## Decisão
Um **Backend for Frontend** (`web-bff`) desde a fase 01. Ele é o **único** backend que o frontend conhece e serve a loja (`/api/v1/store/**`) e o painel (`/api/v1/admin/**`).

O que o BFF faz:
- só **compõe e adapta** dados para as telas;
- define DTOs **por tela**;
- agrega chamadas em paralelo com `StructuredTaskScope`;
- aplica **degradação parcial** quando uma fonte opcional falha;
- repassa uploads em streaming.

O que o BFF não tem:
- **regra de negócio**;
- **banco**;
- **estado**.

Na F02, o Gateway entra **na frente** do BFF; o ADR do F01-051 detalha a divisão borda × experiência.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Frontend chama os serviços | Menos um componente | Frontend acoplado à topologia; N chamadas por tela; CORS e versões internas expostas |
| Agregação dentro de um dos serviços | Sem serviço novo | Serviço de domínio passa a conhecer telas e outros serviços |
| BFFs separados (loja e admin) | Isolamento por experiência | Duplicação prematura (registrado em IF-013) |
| **Um BFF para loja e admin** | Contrato por tela; topologia escondida | Risco de regra de negócio "vazar" para o BFF |

## Consequências
- **Positivas:** o frontend gera seu cliente de um único OpenAPI (ADR-013); falhas de serviços aparecem como degradação controlada nas telas.
- **Negativas / custos:** um salto de rede a mais por requisição; o BFF depende de todos os serviços (resiliência obrigatória, ADR-009).
- **O que observar:** validações de negócio aparecendo no BFF; um BFF único ficando grande demais para loja e admin (IF-013).
