# ADR-015 — Carrinho no navegador, pagamento simulado e frete fixo

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D15 · IF-001 · IF-003 · IF-004 · F01-046 · F01-063 |

## Contexto
O foco do estudo é arquitetura (serviços, consistência, resiliência, nuvem), não as regras comerciais de um e-commerce real. Carrinho no servidor, gateway de pagamento e cálculo de frete trazem integrações e estados que desviam esse foco.

## Decisão
- **Carrinho só no navegador** (Zustand persistido em `localStorage`), enviado inteiro na criação do pedido.
- **Pagamento simulado** com o padrão **Strategy** (PIX, cartão, boleto), regra fixa de aprovação e recusa e várias tentativas por pedido; no máximo um pagamento `APPROVED` por pedido.
- **Frete fixo** definido por configuração (`config-repo`).

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Carrinho no servidor | Carrinho entre dispositivos; reserva de estoque | Mais estado, sessão e limpeza de carrinhos abandonados |
| Gateway real em sandbox | Realismo | Conta externa, webhooks e assincronia, que contrariam o ADR-006 |
| **Simplificações explícitas** | Foco na arquitetura; regras registradas como ideias futuras | Distância de um e-commerce de produção |

## Consequências
- **Positivas:** fluxo completo de ponta a ponta já na fase 01; o Strategy deixa o pagamento real pronto para entrar como nova estratégia.
- **Negativas / custos:** o carrinho não sobrevive à troca de navegador; não há reserva de estoque antes do checkout.
- **O que observar:** as evoluções estão registradas como IF-001 (carrinho no servidor), IF-003 (frete real) e IF-004 (gateway em sandbox).
