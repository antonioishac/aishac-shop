# web-bff

Backend for Frontend do `aishac-shop-web`. **Sem banco de dados e sem estado.** Ver rule `bff`.

## Pacotes

```text
com.aishac.bff/
├── store/          # endpoints /api/v1/store/** (loja)
├── admin/          # endpoints /api/v1/admin/** (painel)
├── client/         # HTTP Interfaces para customer-service e sales-service + Resilience4j
└── infrastructure/
```

## Regras

- Só **compõe e adapta** dados para telas. Qualquer validação de negócio pertence aos serviços.
- DTOs de resposta desenhados **por tela**, nunca repassando o DTO interno do serviço.
- Agregação de várias chamadas em paralelo com `StructuredTaskScope` (Virtual Threads).
- Degradação parcial: se uma fonte opcional falhar, responda com o restante e sinalize no payload (ex.: `"customer": null` + `"warnings": [...]`). Fontes obrigatórias falham a requisição com `ProblemDetail`.
- Upload de imagem: repassa o multipart em streaming ao sales-service, sem carregar o arquivo inteiro em memória.
- O frontend gera seu cliente a partir do OpenAPI **deste** serviço. Mudança de contrato → regenerar o cliente do frontend.
- Testes de contrato **consumidor** contra os stubs dos dois serviços.
