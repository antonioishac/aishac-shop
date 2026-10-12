# ADR-006 — Fase 01 100% síncrona, sem Gateway nem autenticação

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D06 · D24 · premissas P4 e P5 |

## Contexto
O estudo é progressivo: cada fase resolve limitações **observadas** na anterior. Introduzir mensageria, Gateway ou autenticação desde o início esconderia os problemas que justificam essas peças (acoplamento temporal, falhas em cascata, segurança na borda).

## Decisão
Na fase 01, toda comunicação é **HTTP síncrona** (web → web-bff → serviços; sales → customer). Ficam **proibidos**: broker de mensagens, Outbox, eventos de domínio publicados e `ApplicationEventPublisher`. **API Gateway** (F02) e **Keycloak** (F03) também não entram. Necessidades dessas fases são registradas em `docs/backlog/ideias-futuras.md`, não antecipadas. O diagrama da fase também é síncrono.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Eventos desde o início | Arquitetura "final" mais cedo | Não há limitação observada que justifique; o estudo perde o "antes" |
| **Síncrono primeiro, evoluir por fases** | Base simples; limitações reais documentadas em `limitacoes.md` | Problemas conhecidos ficam expostos de propósito |

## Consequências
- **Positivas:** fluxo fácil de depurar; o de-para entre fases fica claro.
- **Negativas / custos:** acoplamento temporal (se o customer-service cai, não se cria pedido); sem autenticação, o `/admin` fica aberto no ambiente local.
- **O que observar:** registrar em `limitacoes.md` as falhas em cascata, as latências somadas e a ausência de segurança. São a entrada das fases 02 a 04.
