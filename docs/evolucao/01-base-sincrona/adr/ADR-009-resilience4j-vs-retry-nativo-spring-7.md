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

## Emenda — 2026-10-13: exceção para o S3 (F01-030)

A decisão acima continua valendo para as chamadas HTTP (sales → customer, BFF → serviços). **Nas chamadas ao S3, o Resilience4j entra só com o Circuit Breaker.** Retry e timeouts ficam com o próprio AWS SDK v2.

**Motivo:** o `S3Client` do SDK já faz retry com backoff (estratégia padrão) e tem timeouts próprios. Somar o Retry do Resilience4j por cima multiplicaria as tentativas (cada tentativa do Resilience4j dispararia as tentativas do SDK) e o TimeLimiter duplicaria um teto que o SDK já aplica, com o risco de cortar uma chamada no meio de um retry interno.

Como ficou no código (`sales-service`, módulo `catalog`):

| Mecanismo | Onde | Configuração |
|---|---|---|
| Retry com backoff | AWS SDK (`S3Client`, estratégia padrão) | Sem ajuste |
| Timeout por tentativa | `StorageConfiguration` (`apiCallAttemptTimeout`) | 10 s (folga para um upload de 5 MB em rede lenta) |
| Timeout total da chamada, incluindo retries | `StorageConfiguration` (`apiCallTimeout`) | 30 s |
| Circuit Breaker | `S3ProductImageStorage` (`@CircuitBreaker(name = "s3")` em `put` e `delete`) | `config-repo/sales-service.properties` (`resilience4j.circuitbreaker.instances.s3.*`) |

- Falha do SDK ou circuito aberto → fallback lança `BusinessException(IMAGE_STORAGE_UNAVAILABLE)` → **`503` em ProblemDetail**, como na decisão original.
- A URL pré-assinada é calculada localmente pelo `S3Presigner` (sem chamada de rede), por isso fica fora do Circuit Breaker; erro do SDK nela também vira `IMAGE_STORAGE_UNAVAILABLE`.
- A rule `storage` foi alinhada a esta emenda.
- **O que observar:** com o retry do SDK, uma chamada lenta pode levar até 30 s antes de o Circuit Breaker contar a falha; o limite de chamada lenta do circuito (`slow-call-duration-threshold`) ajuda a abrir antes disso.
