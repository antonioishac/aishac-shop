# ADR-004 — Arquitetura hexagonal leve, organizada por feature

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D04 · rule `architecture-boundaries` · F01-013 |

## Contexto
O domínio (CPF, estoque, máquina de estados do pedido) precisa ser testável sem Spring e sobreviver às trocas de infraestrutura previstas nas fases seguintes (eventos, nuvem). Organizar por camada técnica (`controllers/`, `services/`) espalha cada funcionalidade pelo projeto inteiro.

## Decisão
Pacotes **por feature**. Dentro de cada feature, as camadas são `domain` ← `application` (casos de uso e ports) ← `adapter/in/web` e `adapter/out/*`. O **modelo de domínio é separado da entidade JPA**, com conversão pelo MapStruct. Configuração transversal fica em `infrastructure/`.

As fronteiras são **verificadas pelo ArchUnit** (`ArchitectureTest` em cada serviço):
- direção das camadas;
- domínio sem Spring, JPA ou Jackson;
- adapters de entrada e de saída sem dependência mútua;
- controllers sem acesso a repositórios;
- sem ciclos entre features;
- sem injeção por campo.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Camadas técnicas clássicas | Familiar; menos classes | Domínio acoplado ao JPA e ao Spring; feature espalhada |
| Hexagonal "completa" (módulos Maven por camada) | Fronteira garantida pelo compilador | Cerimônia excessiva para o tamanho do projeto |
| **Hexagonal leve por feature + ArchUnit** | Domínio puro; feature coesa; fronteiras testadas | Mapeamentos domínio ↔ JPA ↔ DTO |

## Consequências
- **Positivas:** testes de domínio rápidos e sem Spring; trocar persistência ou transporte não toca o domínio.
- **Negativas / custos:** mais classes e mappers por feature.
- **O que observar:** mappers triviais demais (sinal de modelo anêmico); regras do ArchUnit sendo contornadas com exceções.
