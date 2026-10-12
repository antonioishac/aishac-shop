---
name: frontend-feature
description: Cria uma feature do aishac-shop-web (página, componentes, hooks TanStack Query, schema Zod, store Zustand quando necessário e testes) a partir do OpenAPI do web-bff. Use para qualquer nova tela ou fluxo do frontend.
argument-hint: "[area/feature, ex.: store/checkout]"
---

# Feature de frontend: $ARGUMENTS

Siga a rule `react-frontend`.

1. Confirme que os endpoints existem no OpenAPI do `web-bff`. Se não existirem, pare: especifique primeiro no BFF.
2. `npm run generate:api`.
3. Crie em `src/features/$ARGUMENTS/`:
   - `hooks/` — `useXxxQuery`/`useXxxMutation` com chaves em `queryKeys.ts`; invalidações após mutações.
   - `schemas.ts` — Zod para formulários.
   - `components/` e `pages/` — com estados de carregando, vazio e erro.
   - Store Zustand só se houver estado de cliente que sobrevive à navegação.
4. Rota registrada no roteador; links de navegação.
5. Testes: Vitest + Testing Library + MSW para cada estado da tela; Playwright se fizer parte de fluxo crítico (compra, cadastro de produto).
6. `npm run lint && npm run test && npm run build`.
