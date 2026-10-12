# aishac-shop-web

Frontend React + Vite + **TypeScript**. Fala **apenas com o `web-bff`**. Ver rule `react-frontend`.

## Estrutura

```text
src/
├── api/            # cliente gerado do OpenAPI do BFF (não editar à mão) + instância Axios
├── features/       # store/catalog, store/cart, store/checkout, store/orders, admin/*
│   └── <feature>/  # components/, hooks/ (TanStack Query), schemas (Zod), pages/
├── stores/         # Zustand: cart, session
├── components/     # componentes compartilhados de UI
└── lib/            # utilidades (formatação de dinheiro BRL, datas)
```

## Regras

- Dados do servidor: **TanStack Query**. Estado do cliente (carrinho, sessão): **Zustand**. Não misture.
- Dinheiro chega como string decimal; formate com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`. Nunca faça aritmética de preço para enviar ao servidor.
- Formulários: React Hook Form + Zod.
- Erros: o interceptor converte `ProblemDetail` em mensagem amigável.

## Comandos

`npm run dev` · `npm run test` · `npm run lint` · `npm run build` · `npm run generate:api` · `npm run e2e`
