# ADR-009 — Resilience4j nas chamadas externas, e não o retry nativo do Spring 7

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D10 · F01-043 · F01-036 · F01-053 |

## Contexto
O sales-service depende do customer-service; o BFF depende dos dois; o catálogo depende do S3. Na fase síncrona (ADR-006), a lentidão ou queda de uma dependência se propaga. O **Spring Framework 7** passou a ter resiliência nativa: `RetryTemplate`/`RetryPolicy` em `org.springframework.core.retry` e as anotações `@Retryable` e `@ConcurrencyLimit` (`@EnableResilientMethods`). Por isso, a escolha da biblioteca deixou de ser óbvia.

## Decisão
Usar o **Resilience4j** em toda chamada a dependência externa (serviço a serviço, BFF a serviços e S3), combinando:
- **Circuit Breaker**: para de chamar quem está falhando;
- **Retry com backoff**: só para falhas transitórias e operações idempotentes;
- **TimeLimiter**: teto de latência por chamada.

Quando o circuito abre ou o limite estoura, a resposta é **`503` em ProblemDetail**. A configuração fica no `config-repo`, por perfil (skill `configurar-resilience`). O retry nativo do Spring 7 fica reservado a retentativas **locais e simples**, sem dependência remota, se surgirem.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Retry nativo do Spring 7 | Sem dependência extra; API do próprio framework | **Não tem** circuit breaker nem time limiter; retry sozinho amplifica a carga sobre quem já está falhando |
| Spring Retry (projeto separado) | Maduro | Em modo de manutenção; mesma limitação de não ter circuit breaker |
| **Resilience4j** | Circuit breaker, retry, time limiter, bulkhead e rate limiter; métricas no Actuator/Micrometer | Dependência e configuração a mais |

## Consequências
- **Positivas:** falhas da dependência são contidas e observáveis (estados do circuito no Actuator, estudo F01-050).
- **Negativas / custos:** mais configuração por cliente HTTP; risco de combinações ruins (retry dentro do time limiter, retry em operação não idempotente).
- **O que observar:** circuito abrindo com frequência; retries multiplicando a latência. Detalhe: o config client do Spring Cloud ainda usa o `spring-retry` para buscar a configuração na subida (dependência incluída só para isso).
