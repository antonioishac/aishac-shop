# ADRs — Fase 01 (Base síncrona)

Decisões de arquitetura da fase. A numeração é contínua entre fases. Para registrar uma nova decisão, use a skill `registrar-adr`.

| ADR | Decisão | Origem |
|---|---|---|
| [ADR-001](ADR-001-monorepo-com-build-independente.md) | Monorepo com build independente por serviço | D01 |
| [ADR-002](ADR-002-stack-base-java-25-spring-boot-4.md) | Java 25, Spring Boot 4.1 e Spring Cloud 2025.1 | D02 |
| [ADR-003](ADR-003-lombok-com-regras-de-uso.md) | Lombok mantido, com regras de uso | D03 |
| [ADR-004](ADR-004-arquitetura-hexagonal-por-feature.md) | Hexagonal leve por feature, verificada pelo ArchUnit | D04 |
| [ADR-005](ADR-005-dois-microservicos-catalogo-como-modulo.md) | Dois microserviços; catálogo como módulo de vendas | D05 |
| [ADR-006](ADR-006-fase-01-100-por-cento-sincrona.md) | Fase 01 100% síncrona, sem Gateway nem autenticação | D06 · D24 |
| [ADR-007](ADR-007-banco-por-servico-e-convencoes.md) | Um banco por serviço e convenções de modelagem | D07 · D08 |
| [ADR-008](ADR-008-erros-em-problem-detail.md) | Erros em ProblemDetail com catálogo de códigos | D09 |
| [ADR-009](ADR-009-resilience4j-vs-retry-nativo-spring-7.md) | Resilience4j, e não o retry nativo do Spring 7 | D10 |
| [ADR-010](ADR-010-imagens-de-produto-no-s3.md) | Imagens no S3 com upload pelo backend | D11 |
| [ADR-011](ADR-011-localstack-local-e-aws-real-no-dev.md) | LocalStack no local; AWS real no dev | D12 |
| [ADR-012](ADR-012-configuracao-externa-com-config-server.md) | Config Server próprio, backend native e git | D13 |
| [ADR-013](ADR-013-stack-do-frontend.md) | React, TypeScript e cliente gerado do OpenAPI | D14 |
| [ADR-014](ADR-014-autoridade-de-preco-e-dinheiro.md) | Servidor como autoridade de preço; dinheiro como string | D17 · D19 |
| [ADR-015](ADR-015-escopo-simplificado-de-negocio.md) | Carrinho no navegador, pagamento simulado, frete fixo | D15 |
| [ADR-016](ADR-016-cliente-desativado-e-nao-excluido.md) | Cliente desativado, nunca excluído | D18 |
| [ADR-017](ADR-017-idempotencia-na-criacao-de-pedido.md) | Idempotency-Key na criação de pedido | D20 |
| [ADR-018](ADR-018-mcp-do-postgresql-com-usuario-restrito.md) | MCP do PostgreSQL com usuário restrito e auditado | D21 |
| [ADR-019](ADR-019-sem-eureka-descoberta-por-dns.md) | Sem Eureka: descoberta por DNS | D22 |
| [ADR-020](ADR-020-idioma-e-documentacao-por-fase.md) | Inglês no código, português na documentação, diagrama por fase | D16 · D23 |
| [ADR-021](ADR-021-bff-na-fase-01.md) | BFF já na fase 01 | D26 |
| [ADR-022](ADR-022-versionamento-de-api-nativo-spring-7.md) | Versionamento de API nativo do Spring 7 | F01-012 |
| [ADR-023](ADR-023-logs-json-correlacao-e-mascara.md) | Logs JSON, ID de correlação e máscara de dados pessoais | F01-012 |
| [ADR-024](ADR-024-estrategia-de-testes-gates-e-ci.md) | Testes, gates de qualidade e CI por serviço | D25 · F01-013 · F01-014 |

Decisões D01–D26 do registro de concepção (`docs/backlog/README.md`): todas cobertas acima.
