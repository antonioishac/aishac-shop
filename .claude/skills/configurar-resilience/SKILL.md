---
name: configurar-resilience
description: Configura Resilience4j (Circuit Breaker, Retry, TimeLimiter, Rate Limiter) para uma chamada externa — entre serviços, do BFF ou ao S3 — com fallback em ProblemDetail, propriedades nos três perfis e testes com WireMock. Use ao criar ou alterar qualquer cliente de dependência externa.
argument-hint: "[servico] [nome-da-instancia, ex.: customerService]"
arguments: [service, instance]
---

# Resiliência para `$instance` em `$service`

1. **Classifique a operação:** idempotente (GET, PUT, DELETE, POST com `Idempotency-Key`) ou não. **Retry só em idempotente.**
2. **Anotações** no adapter (nunca no caso de uso):
   ```java
   @CircuitBreaker(name = "$instance", fallbackMethod = "fallback")
   @Retry(name = "$instance")
   ```
   Timeout via `TimeLimiter` ou timeout de leitura do `RestClient` (prefira o do cliente HTTP para chamadas síncronas em Virtual Threads).
3. **Fallback:** lança uma exceção de infraestrutura (`DependencyUnavailableException`) traduzida para `503` com `code = <DEPENDENCIA>_UNAVAILABLE`. Nunca invente dados no fallback de fonte obrigatória.
4. **Propriedades** (skill `configurar-ambiente`), prefixo `resilience4j.*.instances.$instance`:
   - circuitbreaker: `sliding-window-size=20`, `failure-rate-threshold=50`, `wait-duration-in-open-state=10s`, `permitted-number-of-calls-in-half-open-state=3`, `slow-call-duration-threshold=2s`.
   - retry: `max-attempts=3`, `wait-duration=200ms`, backoff exponencial com jitter, `retry-exceptions` só para erros de rede e 5xx (nunca 4xx).
   - Valores de `local`/`dev`/`prod` podem diferir; documente no `plan.md`.
5. **Observabilidade:** expor `circuitbreakers` e `retries` no Actuator; métricas via Micrometer.
6. **Testes** (WireMock): sucesso; 5xx com retry e sucesso na 2ª tentativa; falhas consecutivas abrindo o circuito; fallback retornando 503; 4xx sem retry.
7. Registre um ADR se for a primeira instância (decisão D10: Resilience4j vs retry nativo do Spring 7).
