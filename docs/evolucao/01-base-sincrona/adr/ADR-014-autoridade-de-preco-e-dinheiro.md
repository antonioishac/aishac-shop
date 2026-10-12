# ADR-014 — Servidor como autoridade de preço; dinheiro como string decimal

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D17 · D19 · F01-042 · rule `java-style` |

## Contexto
Um cliente adulterado pode enviar preço ou total menores. Além disso, `double` e o `number` do JavaScript não representam centavos com exatidão (`0.1 + 0.2`), e cada camada pode arredondar de um jeito.

## Decisão
- O **servidor é a única autoridade sobre preço e total**. O frontend envia apenas `productId` e `quantity`; o sales-service calcula itens, frete e total e grava um *snapshot* do preço no pedido.
- Dinheiro trafega na API como **string decimal** (`"129.90"`), moeda BRL.
- No Java é **`BigDecimal`**, com escala 2 e `RoundingMode.HALF_EVEN`, encapsulado no Value Object `Money` quando houver operações.
- No banco é `NUMERIC(19,2)`, com `CHECK` de valores positivos.
- O frontend só **formata** (`Intl.NumberFormat('pt-BR', BRL)`), nunca calcula valor para enviar.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Frontend envia preço e o servidor confere | Detecta preço desatualizado na tela | Dois cálculos para manter iguais; brecha se a conferência falhar |
| Dinheiro como número JSON | Natural no JSON | Perda de precisão em JavaScript e em desserializadores |
| Dinheiro em centavos (inteiro) | Sem decimais | Menos legível; conversões em toda exibição |
| **Servidor calcula; string decimal** | Sem ambiguidade nem perda; uma única regra | Conversão string ↔ `BigDecimal` nas bordas |

## Consequências
- **Positivas:** impossível comprar com preço adulterado; arredondamento definido em um só lugar.
- **Negativas / custos:** o carrinho no navegador mostra um total estimado, e o valor definitivo só existe após a criação do pedido.
- **O que observar:** alguém somando preços no frontend para exibir "o total"; uso de `double` em qualquer camada.
