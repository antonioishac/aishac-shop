---
paths:
  - "web/**"
---

# Frontend React + TypeScript

- **TypeScript estrito** (`strict: true`); proibido `any` (use `unknown` + narrowing).
- Componentes funcionais com hooks; um componente por arquivo; nomes em PascalCase.
- Organização por feature em `src/features/<area>/<feature>/`.
- **API:** somente pelo cliente gerado do OpenAPI do BFF (`npm run generate:api`). Nunca escreva URL de serviço interno nem chame `axios` cru em componente.
- **Dados do servidor:** TanStack Query (hooks `useXxxQuery` / `useXxxMutation` por feature, chaves de query centralizadas). **Estado de cliente:** Zustand (`cart`, `session`), persistido em `localStorage` com `try/catch`.
- **Formulários:** React Hook Form + Zod; o schema Zod é a fonte da validação do formulário.
- **Dinheiro:** recebido como string; exibido com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`. O frontend nunca envia preço ou total ao servidor.
- **Checkout:** gere uma `Idempotency-Key` (UUID) por tentativa de compra e reutilize-a em reenvios da mesma tentativa.
- **UI:** Tailwind + Lucide; acessibilidade (rótulos, foco visível, contraste, navegação por teclado); estados de carregando, vazio e erro em toda tela de dados.
- Textos da interface em português; código em inglês.
