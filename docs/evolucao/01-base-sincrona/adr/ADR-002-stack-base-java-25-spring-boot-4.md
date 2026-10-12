# ADR-002 — Stack base: Java 25, Spring Boot 4.1 e Spring Cloud 2025.1

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D02 · F01-011 · F01-012 |

## Contexto
O projeto é um estudo de referência e deve usar a plataforma atual, que muda pontos importantes: Virtual Threads e Structured Concurrency (Java 25 LTS); modularização do Spring Boot 4; Jackson 3; JSpecify; versionamento de API e resiliência nativos no Spring Framework 7.

## Decisão
**Java 25**, **Spring Boot 4.1.x** (4.1.1 na criação) e **Spring Cloud 2025.1.x** (2025.1.3 na criação), com Maven. Os projetos são gerados pelo Spring Initializr. Virtual Threads ficam ligadas em todos os serviços (`spring.threads.virtual.enabled=true`). Null-safety com JSpecify (`@NullMarked` por pacote).

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Java 21 + Boot 3.5 | Ecossistema mais maduro; mais material | Estuda uma plataforma que já está sendo substituída |
| **Java 25 + Boot 4.1** | LTS atual; recursos novos do Spring 7 | Bibliotecas de terceiros ainda acompanhando (ex.: springdoc 3) |

## Consequências
- **Positivas:** acesso a Virtual Threads, `StructuredTaskScope`, versionamento de API nativo e starters modulares.
- **Negativas / custos:** algumas bibliotecas exigem versão específica (springdoc 3.x); APIs mudaram de pacote (ex.: `RestClientCustomizer` em `org.springframework.boot.restclient`); material de referência mais escasso.
- **O que observar:** incompatibilidades em bibliotecas novas; *pinning* de Virtual Threads (`synchronized` em drivers e bibliotecas).
