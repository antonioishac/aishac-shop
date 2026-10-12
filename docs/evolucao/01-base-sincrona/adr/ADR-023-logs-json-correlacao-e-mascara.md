# ADR-023 — Logs JSON, ID de correlação e máscara de dados pessoais

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | F01-012 · IF-020 · rule `java-style` (logs) |

## Contexto
Uma requisição atravessa web → BFF → serviços. Sem um identificador comum, não dá para seguir o caminho nos logs. Logs em texto livre são difíceis de filtrar e tendem a vazar CPF e e-mail, dados protegidos pela LGPD. Observabilidade completa (Prometheus, Tempo, Loki) fica para depois (IF-020).

## Decisão
- **Logs estruturados em JSON (ECS)** em todos os perfis, pelo suporte nativo do Spring Boot (`logging.structured.format.console=ecs`).
- **ID de correlação:** o `CorrelationIdFilter` lê `X-Correlation-Id`, aceitando só valores seguros (até 64 caracteres, `[A-Za-z0-9-]`), ou gera um UUID. O ID vai para o MDC (e daí para cada linha de log), volta no cabeçalho da resposta e entra no ProblemDetail. Nas chamadas de saída, um interceptor do `RestClient` o propaga.
- **Máscara de CPF e e-mail** aplicada a **todo valor texto** do log JSON (mensagem, MDC, exceção) por um `StructuredLoggingJsonMembersCustomizer`. É uma rede de segurança: a regra continua sendo não logar dados pessoais.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Logback com encoder de terceiros (logstash-logback-encoder) | Muito usado | Dependência extra; o Boot já faz JSON nativamente |
| Micrometer Tracing + OpenTelemetry já na fase 01 | Trace distribuído real | Antecipa a stack de observabilidade (IF-020) |
| Máscara só por disciplina no código | Nada a configurar | Um único `log.info(customer)` vaza dados |
| **JSON nativo + ID de correlação + máscara global** | Simples; rastreável entre serviços; vazamento contido | A regex de máscara pode errar em casos de borda |

## Consequências
- **Positivas:** `grep` por `correlationId` reconstrói a requisição entre serviços; ProblemDetail e log se ligam pelo mesmo ID.
- **Negativas / custos:** logs locais menos legíveis a olho; a máscara custa uma regex por valor logado.
- **O que observar:** números de 11 dígitos que não são CPF sendo mascarados; quando o ID de correlação deixar de bastar, adotar trace distribuído (IF-020).
