# ADR-020 — Código em inglês, documentação em português, diagrama por fase

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D16 · D23 · F01-009 |

## Contexto
O código segue convenções universais (bibliotecas, mensagens de erro e commits em inglês), mas o estudo, as decisões e as specs são lidos e aprovados por quem fala português. A arquitetura vai mudar a cada fase, e o valor do estudo está em comparar o "antes" e o "depois".

## Decisão
- **Código, identificadores, mensagens de log e commits em inglês**; **documentação, specs, ADRs e textos da interface em português**.
- **Um diagrama draw.io versionado por fase** em `docs/evolucao/<fase>/`, em XML legível no diff. Ele traz C4 (contexto, containers, componentes), sequências dos fluxos críticos, máquina de estados, deployment e ER.
- Cada fase fecha com diagrama, ADRs, `limitacoes.md`, `de-para.md` (a partir da F02) e tag git.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Tudo em português | Uniforme | Mistura idiomas com APIs e bibliotecas; nomes estranhos no código |
| Tudo em inglês | Uniforme | Atrito na leitura e aprovação das specs |
| Diagrama único sempre atualizado | Um arquivo só | Perde o histórico; não há "foto" de cada fase para o de-para |
| **Inglês no código, português na documentação, diagrama por fase** | Cada público no seu idioma; histórico arquitetural preservado | Duas línguas no repositório; mais diagramas para manter |

## Consequências
- **Positivas:** o de-para entre fases fica concreto, comparando arquivos.
- **Negativas / custos:** cada fase copia e evolui o diagrama da anterior.
- **O que observar:** termos de negócio traduzidos de formas diferentes entre código e specs (ex.: "pedido" × `order`). Se começar a acontecer, criar um glossário de domínio.
