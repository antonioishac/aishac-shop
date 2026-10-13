# Convenções do diagrama draw.io

## Páginas da fase 01
1. C4 Contexto
2. C4 Containers
3. Componentes — sales-service
4. Componentes — customer-service
5. Componentes — web-bff
6. Sequência — Criar Pedido
7. Sequência — Detalhe do Pedido (agregação e degradação)
8. Sequência — Imagens (inserir, substituir, excluir)
9. Máquina de estados — Pedido
10. Máquina de estados — Cliente
11. Deployment — Local (Docker Compose)
12. Deployment — Kubernetes (alvo)
13. ER — aishac_customer
14. ER — aishac_sales

## Cores (preenchimento)
| Elemento | Cor |
|---|---|
| Pessoa / ator | `#08427B` (texto branco) |
| Container do sistema (serviço, BFF, web) | `#1168BD` (texto branco) |
| Banco de dados | `#2E7D32` (texto branco), forma de cilindro |
| Sistema externo / AWS | `#999999` (texto branco) |
| Componente interno | `#85BBF0` (texto escuro) |
| Infra de suporte (config-server, LocalStack) | `#6A1B9A` (texto branco) |

## Linhas
- Chamada síncrona: seta sólida, rótulo `HTTP/JSON` + verbo/rota principal.
- Referência lógica (sem FK / outro serviço): seta **tracejada**.
- Fluxo de falha/fallback em sequência: seta tracejada vermelha `#C62828`.

## Texto
- Título de cada página no canto superior esquerdo: `<Página> — Fase 01 (Base síncrona)`.
- Legenda em toda página de C4 e de ER.
- Nomes técnicos em inglês; descrições em português.
