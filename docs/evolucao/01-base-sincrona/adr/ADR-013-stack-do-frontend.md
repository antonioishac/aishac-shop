# ADR-013 — Stack do frontend: React, TypeScript e cliente gerado do OpenAPI

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D14 · F01-060 · F01-061 · rule `react-frontend` |

## Contexto
A loja e o painel `/admin` precisam de formulários validados, listas paginadas, carrinho persistente e tratamento uniforme de erros, consumindo apenas o `web-bff`. Contratos escritos à mão no frontend divergem do backend sem aviso.

## Decisão
- **React + Vite + TypeScript** (`strict`), **Tailwind** e **Lucide**.
- **TanStack Query** para dados do servidor; **Zustand** só para estado do cliente (carrinho, sessão).
- **Axios com cliente gerado do OpenAPI do BFF** (`npm run generate:api`): o frontend não escreve URLs nem tipos de API à mão. Um interceptor propaga o ID de correlação e converte ProblemDetail em mensagem amigável.
- **React Hook Form + Zod** em formulários.
- Testes com Vitest + Testing Library (com MSW) e E2E com Playwright.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Next.js | SSR e roteamento integrados | SSR não é objetivo do estudo; mais uma camada servidor ao lado do BFF |
| Redux Toolkit para tudo | Um único padrão de estado | Mistura cache de servidor com estado de interface; mais boilerplate |
| **Vite SPA + Query + Zustand + cliente gerado** | Separação clara de estados; contrato verificado pelo compilador | Depende de o OpenAPI do BFF estar correto |

## Consequências
- **Positivas:** uma mudança de contrato no BFF quebra a compilação do frontend, não a produção.
- **Negativas / custos:** é preciso regenerar o cliente a cada mudança no BFF; duas bibliotecas de estado para aprender.
- **O que observar:** estado de servidor sendo copiado para o Zustand; chamadas Axios "cruas" fora do cliente gerado.
