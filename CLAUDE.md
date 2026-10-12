# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Aishac Shop

E-commerce de estudo, construído de ponta a ponta como projeto de referência.
Backlog e decisões: @docs/backlog/README.md

**Estado atual:** só a estrutura do Claude Code e a documentação existem; os serviços e o frontend ainda não foram gerados (cada pasta tem apenas um `CLAUDE.md`). O primeiro passo é o marco 01.1 do backlog (`docs/backlog/fase-01-base-sincrona.md`).

## Onde estão as instruções

- `CLAUDE.md` em cada serviço e em `web/aishac-shop-web/`: pacotes e regras do contexto. Leia o do serviço antes de mexer nele.
- `.claude/rules/`: regras por tipo de arquivo, carregadas por `paths` (testes, Flyway, OpenAPI, BFF, React, Lombok, storage...).
- `.claude/skills/`: `sdd-feature` só é acionada pelo usuário (`/sdd-feature F01-0XX`); as demais você pode usar diretamente.
- `.claude/hooks/`: `guard-bash.sh` e `guard-files.sh` bloqueiam comandos e arquivos proibidos; `on-stop.sh`, ao fim de cada tarefa, roda `spotless:apply` + `./mvnw test` nos serviços alterados e `lint` + `test` no frontend, e devolve a falha para correção. Os hooks dependem de `bash`, `jq` e de um repositório git (sem git, o `on-stop` não faz nada).

## Mapa do repositório

| Caminho | Conteúdo |
|---|---|
| `services/customer-service/` | Microserviço de clientes (banco `customers_db`) |
| `services/sales-service/` | Microserviço de vendas: módulos `catalog`, `order`, `payment` (banco `sales_db`) |
| `services/web-bff/` | BFF do frontend. Só compõe e adapta; sem regra de negócio, sem banco |
| `services/config-server/` | Spring Cloud Config Server |
| `config-repo/` | Configuração por serviço e perfil (`local`, `dev`, `prod`) |
| `web/aishac-shop-web/` | Frontend React + TypeScript (fala só com o BFF) |
| `infra/docker/` | Arquivos Docker Compose (executados **apenas pelo usuário**) |
| `infra/aws/` | Scripts AWS CLI (executados **apenas pelo usuário**) |
| `specs/` | Specs do SDD, uma pasta por feature |
| `docs/backlog/` | Backlog mestre (fonte da verdade do que fazer) |
| `docs/evolucao/<fase>/` | Diagrama draw.io, ADRs, `limitacoes.md` e `de-para.md` de cada fase |

Fluxo de chamadas na fase 01: `web → web-bff → customer-service | sales-service`. Vendas chama Clientes via REST.

## Stack

Java 25 · Spring Boot 4.1.x · Spring Cloud 2025.1.x · Maven (wrapper por serviço) · PostgreSQL + Flyway · MapStruct + Lombok · Resilience4j · OpenAPI · JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit, Spring Cloud Contract · AWS SDK v2 (S3) · React + Vite + TypeScript, Tailwind, Zustand, TanStack Query.

## Premissas inegociáveis

1. **Nunca execute `docker` ou `docker compose`.** Escreva os arquivos e informe os comandos; o usuário executa. Testcontainers nos testes é permitido.
2. **Nunca leia, crie com valores reais ou peça credenciais.** Segredos só por variável de ambiente (`${VAR}`). `.env` é do usuário.
3. **Nunca execute `terraform apply` ou `destroy`.** Apenas `plan`.
4. **Estrutura de banco só por migration Flyway.** O MCP do PostgreSQL serve para consultar e manipular dados de desenvolvimento, nunca para DDL.
5. **Fase 01 é 100% síncrona.** Não introduza broker, Outbox, eventos de domínio publicados ou `ApplicationEventPublisher`.
6. **Não antecipe fases futuras** (Gateway, Keycloak, Kafka, Terraform). Se algo pedir isso, registre em `docs/backlog/ideias-futuras.md`.

## Princípios da base

- Domínio isolado da infraestrutura (hexagonal leve por feature). Modelo de domínio ≠ entidade JPA.
- Contratos versionados (`/api/v1`) e documentados. Serviços só se conhecem por contrato HTTP; **nenhum import de classe entre serviços**.
- Um banco por serviço; sem FK ou consulta entre bancos.
- Configuração externa; serviços stateless; health checks; graceful shutdown.
- Idempotência e lock otimista nascem no banco.
- O servidor é a autoridade sobre preço e total.
- Código em **inglês**; documentação, specs e ADRs em **português**.

## Fluxo de trabalho: Spec-Driven Development

Toda funcionalidade (`FEAT` no backlog) segue a skill `sdd-feature`:

1. **Spec** (`spec-writer`) → `specs/<contexto>/<id>-<nome>/spec.md` + contrato OpenAPI. ⛔ **Aprovação do usuário.**
2. **Modelo de dados** (`database-migration-specialist`, skill `modelar-dados`). ⛔ **Aprovação do usuário.** Depois, a migration.
3. **Implementação**: `testing-specialist` escreve os testes que falham; `backend-java-engineer` / `frontend-react-engineer` implementam.
4. **Revisão** (`code-reviewer`, contexto limpo) → correções → `architecture-guardian` atualiza diagrama e ADRs.

Antes de começar um item, confira a Definition of Ready; ao terminar, a Definition of Done (ambas em `docs/backlog/README.md`). Atualize o status do item no backlog.

## Estratégia de testes

| Nível | Sufixo | Ferramentas | O que testa |
|---|---|---|---|
| Unitário | `*Test` | JUnit 5, AssertJ, Mockito | Domínio e casos de uso, sem Spring |
| Fatia | `*Test` | `@WebMvcTest`, `@DataJpaTest` + Testcontainers | Um adapter isolado |
| Integração | `*IT` | `@SpringBootTest` + Testcontainers (PostgreSQL, LocalStack) | Fluxo completo do serviço |
| Contrato | `*ContractTest` / gerados | Spring Cloud Contract | Fronteira entre serviços e BFF |
| Arquitetura | `ArchitectureTest` | ArchUnit | Fronteiras de camada e de módulo |
| Frontend | `*.test.tsx` | Vitest, Testing Library | Componentes e hooks |
| E2E | `e2e/*.spec.ts` | Playwright | Fluxos de usuário |

Prefira muitos unitários e poucos de integração. Testes de domínio não sobem Spring.

## Comandos (por serviço, dentro de `services/<servico>/`)

- `./mvnw test` → unitários e de fatia
- `./mvnw verify` → tudo, incluindo `*IT` (Testcontainers)
- `./mvnw spotless:apply` → formatação
- `./mvnw test -Dtest=CustomerTest` (ou `-Dtest='CustomerTest#shouldRejectInvalidCpf'`) → um teste unitário/de fatia
- `./mvnw verify -Dit.test=CreateOrderIT` → um único `*IT` (Failsafe; os unitários também rodam)

No Windows fora do Git Bash, use `mvnw.cmd`. Testes de integração com S3 precisam de `LOCALSTACK_AUTH_TOKEN` no ambiente.

Frontend (`web/aishac-shop-web/`): `npm run test`, `npm run lint`, `npm run build`, `npm run generate:api` (regera o cliente a partir do OpenAPI do BFF; rode após mudar o contrato do BFF). Um teste: `npx vitest run src/features/store/cart`.

## Commits

Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`), com escopo do serviço: `feat(sales): ...`. Detalhes na rule `git-commits`.
