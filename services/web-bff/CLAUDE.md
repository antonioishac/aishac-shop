# web-bff

Backend for Frontend do `aishac-shop-web`. **Sem banco de dados e sem estado.** Ver rule `bff`.

## Pacotes

```text
com.aishac.bff/
├── config/        # Clock, OpenAPI, correlação, Resilience4j
├── controller/    # store/** (loja) e admin/** (painel): sem regra de negócio
├── client/        # HTTP Interfaces para customer-service e sales-service
├── dto/           # DTOs por tela (Records)
├── exception/     # ErrorCode, GlobalExceptionHandler
├── mapper/        # MapStruct: respostas dos serviços → DTOs de tela
└── service/       # composição/agregação (StructuredTaskScope) e degradação parcial
    └── impl/
```

Camadas por tipo, como nos demais serviços (ADR-025). Esqueleto no F01-052.

## Regras

- Só **compõe e adapta** dados para telas. Qualquer validação de negócio pertence aos serviços.
- DTOs de resposta desenhados **por tela**, nunca repassando o DTO interno do serviço.
- Agregação de várias chamadas em paralelo com `StructuredTaskScope` (Virtual Threads).
- Degradação parcial: se uma fonte opcional falhar, responda com o restante e sinalize no payload (ex.: `"customer": null` + `"warnings": [...]`). Fontes obrigatórias falham a requisição com `ProblemDetail`.
- Upload de imagem: repassa o multipart em streaming ao sales-service, sem carregar o arquivo inteiro em memória.
- O frontend gera seu cliente a partir do OpenAPI **deste** serviço. Mudança de contrato → regenerar o cliente do frontend.
- Testes de contrato **consumidor** contra os stubs dos dois serviços.
