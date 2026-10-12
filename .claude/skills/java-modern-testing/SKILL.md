---
name: java-modern-testing
description: Padrões de teste do projeto para Java 25 / Spring Boot 4 — unitários de service, fatias, integração com Testcontainers (PostgreSQL, LocalStack), contrato com Spring Cloud Contract, concorrência com Virtual Threads e ArchUnit. Use ao escrever ou revisar qualquer teste Java.
user-invocable: true
---

# Testes Java modernos

Siga a rule `testing`. Exemplos completos em [examples.md](examples.md).

## Escolha do nível
| Pergunta | Nível |
|---|---|
| Regra de negócio (service)? | Unitário sem Spring: repository mockado (Mockito), mapper real (`Mappers.getMapper`) |
| Tradução de erros do handler? | MockMvc *standalone* com controller de teste |
| Mapeamento JSON, validação, status HTTP? | `@WebMvcTest` |
| Consulta, constraint, migration? | `@DataJpaTest` + Testcontainers |
| Fluxo de ponta a ponta no serviço? | `*IT` com `@SpringBootTest` |
| Fronteira com outro serviço/BFF? | Contrato |
| Estoque, idempotência, lock otimista? | Concorrência com Virtual Threads |

## Base de integração
Uma classe abstrata por serviço (`AbstractIntegrationTest`) com containers estáticos e `@ServiceConnection`, reutilizados entre classes.

## Checklist de um bom teste
- Nome descreve comportamento; Given/When/Then visível.
- AssertJ, incluindo `assertThatThrownBy` e `usingRecursiveComparison`.
- Sem `Thread.sleep`; `Clock` fixo.
- Dados via builders das entidades (`Customer.builder()`) ou fixtures.
- Um motivo para falhar por teste.
