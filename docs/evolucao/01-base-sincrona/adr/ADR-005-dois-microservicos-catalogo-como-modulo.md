# ADR-005 — Dois microserviços; catálogo como módulo do sales-service

| Campo | Valor |
|---|---|
| Status | Aceita (organização dos módulos em pacotes revista pelo ADR-025; definição na spec F01-030) |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D05 · IF-010 |

## Contexto
O e-commerce tem os contextos Clientes, Catálogo, Pedido e Pagamento. Separar todos em serviços multiplica a operação e as chamadas remotas; juntar tudo perde o estudo de comunicação entre serviços. A criação de pedido precisa de preço e estoque **consistentes na mesma transação**.

## Decisão
Dois serviços:
- **`customer-service`**: Clientes.
- **`sales-service`**: Catálogo, Pedido e Pagamento, como módulos.

Os módulos do sales-service têm **fronteira de pacote rígida**:
- só se comunicam por ports declarados no módulo consumidor, implementados por um adapter que chama a camada `application` do outro módulo;
- `catalog` não conhece `order` nem `payment`;
- não há FK entre tabelas de módulos diferentes.

O ArchUnit verifica essas regras.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Um serviço por contexto (4 serviços) | Isolamento máximo | Baixa de estoque vira transação distribuída já na fase 01 |
| Monólito único | Simples | Sem estudo de comunicação entre serviços |
| **2 serviços + monólito modular em vendas** | Estoque e pedido na mesma transação; extração futura preparada | A disciplina entre módulos depende de testes |

## Consequências
- **Positivas:** baixa de estoque atômica no banco; caminho de extração do catálogo pronto (padrão Strangler, IF-010).
- **Negativas / custos:** o sales-service fica maior; os módulos escalam juntos.
- **O que observar:** chamadas entre módulos ficando "tagarelas"; necessidade de escalar o catálogo separado das vendas.
