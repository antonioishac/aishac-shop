# F01 — Base síncrona

**Objetivo de estudo:** construir um e-commerce sólido, testável e bem documentado, com dois microserviços síncronos, que sirva de fundação para todas as fases seguintes.

**Entregável da fase:** loja funcionando de ponta a ponta (catálogo → carrinho → checkout → pedido → pagamento simulado → cancelamento), painel `/admin`, imagens no S3, testes em todos os níveis, CI verde e a "foto" da arquitetura síncrona para o de-para futuro.

**Tag de fechamento:** `fase-01-base-sincrona`

## Marcos

| Marco | Entrega | Depende de |
|---|---|---|
| [01.1 Fundação](#marco-011--fundação) | Monorepo, Claude Code, Docker Compose, Config Server, CI, diagrama | — |
| [01.2 Clientes](#marco-012--clientes) | `customer-service` completo | 01.1 |
| [01.3 Catálogo](#marco-013--catálogo) | Categorias, produtos, estoque, imagens no S3 | 01.1 |
| [01.4 Vendas](#marco-014--vendas) | Pedido, pagamento, cancelamento, resiliência | 01.2, 01.3 |
| [01.5 BFF](#marco-015--bff) | `web-bff`: contrato das telas, composição e degradação parcial | 01.2, 01.3, 01.4 |
| [01.6 Frontend](#marco-016--frontend) | Loja, carrinho, checkout, `/admin` (consumindo só o BFF) | 01.5 |
| [01.7 Fechamento](#marco-017--fechamento) | Carga, revisão, limitações, tag | 01.6 |

---

## Marco 01.1 — Fundação

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-001 | TEC | Estrutura do monorepo: `services/`, `web/`, `config-repo/`, `infra/`, `docs/`, `specs/`, `.claude/` | Claude | ✅ |
| F01-002 | DOC | `CLAUDE.md` raiz (visão, princípios da base, mapa do repositório, fluxo SDD, estratégia de testes, comandos) e `CLAUDE.md` por serviço e do frontend | Claude | ✅ |
| F01-003 | TEC | **Rules** em `.claude/rules/`: `java-style`, `lombok`, `api-openapi`, `database`, `flyway`, `testing`, `architecture-boundaries`, `bff`, `storage`, `configuration`, `docker`, `react-frontend`, `git-commits` | Claude | ✅ |
| F01-004 | TEC | **Agents** em `.claude/agents/`: `spec-writer`, `database-migration-specialist`, `backend-java-engineer`, `testing-specialist`, `frontend-react-engineer`, `code-reviewer` (somente leitura), `architecture-guardian` | Claude | ✅ |
| F01-005 | TEC | **Skills** em `.claude/skills/`: `sdd-feature` (templates spec/plan/tasks, DoR/DoD, portões), `modelar-dados`, `gerar-entidade`, `gerar-flyway`, `configurar-resilience`, `java-modern-testing`, `contrato-openapi`, `integrar-s3`, `configurar-ambiente`, `frontend-feature`, `registrar-adr`, `atualizar-diagrama` | Claude | ✅ |
| F01-006 | TEC | `.claude/settings.json`: **permissões** (negar `docker`/`docker compose`, `terraform apply/destroy`, `git push --force`, leitura de `.env`) e **hooks** (Spotless após edição; bloqueio de edição de migration já commitada; varredura de segredos; testes ao fim da tarefa) | Claude | ✅ |
| F01-007 | TEC | **MCP do PostgreSQL** em `.mcp.json` com URL via variável `CLAUDE_PG_URL`; usuário `claude_mcp` (DML sim, DDL não); `log_statement` para o usuário; escrita sujeita a aprovação. Escolha de servidor MCP mantido e com modo somente leitura | Ambos | 🔄 |
| F01-008 | ADR | ADRs iniciais a partir das decisões D01–D25, incluindo: "Resilience4j vs retry nativo do Spring 7", "Por que não Eureka", "Catálogo como módulo de Vendas", "Hexagonal por feature" | Claude | ✅ |
| F01-009 | DOC | **Diagrama draw.io** da fase em `docs/evolucao/01-base-sincrona/aishac-shop.drawio`, páginas: C4 Contexto · C4 Containers (com o `web-bff`) · Componentes sales-service · Componentes customer-service · Componentes web-bff · Sequência "Criar Pedido" (web → BFF → serviços, com fallback) · Sequência "Detalhe do pedido" (agregação em paralelo e degradação parcial) · Sequência "Upload/Substituição/Exclusão de imagem" · Máquina de estados do Pedido · Deployment local · Deployment Kubernetes (alvo) · ER `aishac_customer` · ER `aishac_sales` | Claude | ✅ |
| F01-010 | TEC | **Docker Compose** (`infra/docker/`): `compose.infra.yml` (PostgreSQL, LocalStack), `compose.apps.yml` (config-server, serviços, web-bff, web), `postgres/init/` (bancos, usuários por serviço, `claude_mcp`, revogação de acesso público), `.env.example`, `README.md` com comandos. **Você executa.** | Claude | ✅ |
| F01-011 | TEC | **Config Server** + `config-repo/` com `application.properties` e arquivos por serviço × perfil (`local`, `dev`, `prod`); `fail-fast` com retry; segredos só via `${VAR}` | Claude | ✅ |
| F01-012 | TEC | **Esqueleto dos serviços** (Boot 4.1, Java 25): Virtual Threads, Actuator (liveness/readiness), graceful shutdown, logs JSON, **ID de correlação** propagado, máscara de CPF/e-mail nos logs, `ProblemDetail` base, OpenAPI, versionamento de API, `Clock` injetável, Lombok + MapStruct, Dockerfile multi-stage | Claude | ✅ |
| F01-013 | TEC | **Gates de qualidade**: ArchUnit (fronteiras de camada e entre módulos), JaCoCo (cobertura mínima), Spotless, varredura de dependências vulneráveis e de segredos; separação `*Test` / `*IT` | Claude | ✅ |
| F01-014 | TEC | **CI** (GitHub Actions): workflow por serviço e frontend com filtro de caminho; build, testes (incluindo Testcontainers) e gates | Claude | ✅ |
| F01-015 | TEC | **Experiência de desenvolvimento**: `Makefile`/Taskfile com comandos (sem executar Docker pelo Claude), coleção de requisições `.http` versionada | Claude | ✅ |
| F01-016 | TEC | Git: estratégia de branches, template de PR, Conventional Commits, CHANGELOG, versionamento semântico por serviço | Claude | ✅ |
| F01-017 | DOC | `docs/evolucao/01-base-sincrona/`: `adr/`, `limitacoes.md` (**modelo vazio**) | Claude | ✅ |
| F01-018 | ESTUDO | Criar conta LocalStack (plano Hobby) e configurar `LOCALSTACK_AUTH_TOKEN` no seu ambiente | Você | ⬜ |
| F01-019 | ESTUDO | Subir a infraestrutura local pela primeira vez e validar acesso aos bancos com os usuários de cada serviço | Você | ⬜ |

---

## Marco 01.2 — Clientes

`customer-service` · banco `aishac_customer`

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-020 | FEAT | **Spec: Cadastro de cliente** — CPF validado (dígitos verificadores) e imutável, e-mail único sem diferenciar maiúsculas, status `ACTIVE`/`BLOCKED`/`INACTIVE` | Ambos | ✅ |
| F01-021 | TEC | Migrations: `V1__create_customers`, `V2__create_customer_addresses` (*exclusion constraint* adiável para o endereço padrão) | Claude | ✅ |
| F01-022 | FEAT | Clientes: criar, buscar por ID, listar paginado (filtros nome/e-mail/status), atualizar, bloquear, **desativar** (sem exclusão física — D18) | Claude | ✅ |
| F01-023 | FEAT | Endereços: adicionar, listar, atualizar, remover, definir endereço padrão | Claude | ✅ |
| F01-024 | FEAT | Endpoint consumido por Vendas: dados para pedido (status do cliente + endereço de entrega) | Claude | ✅ |
| F01-025 | TEC | **Teste de contrato (produtor)** com Spring Cloud Contract para o endpoint do F01-024 | Claude | ✅ |
| F01-026 | TEC | Catálogo de erros do serviço (`CUSTOMER_NOT_FOUND`, `CPF_ALREADY_EXISTS`, …) | Claude | ✅ |

---

## Marco 01.3 — Catálogo

Módulo `catalog` do `sales-service` · banco `aishac_sales`

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-030 | FEAT | **Spec: Catálogo** — categorias (1 nível), produtos, estoque, imagens | Ambos | 🔄 |
| F01-031 | TEC | Migrations: `categories`, `products` (`CHECK price > 0`, `CHECK stock >= 0`), `product_images` (principal única por produto) e **seed separado** de dados de exemplo | Claude | ✅ |
| F01-032 | FEAT | Categorias: criar, listar, atualizar, ativar/desativar | Claude | ✅ |
| F01-033 | FEAT | Produtos: criar, buscar, listar paginado (filtros categoria, nome, faixa de preço, ativo; ordenação), atualizar, ativar/desativar | Claude | ✅ |
| F01-034 | FEAT | Estoque: ajuste manual (entrada/saída) pelo admin, com baixa **atômica** | Claude | ✅ |
| F01-035 | FEAT | **Imagens**: inserir (multipart), listar, buscar, substituir (chave nova), atualizar metadados (principal, ordem, alt), **excluir** (banco → S3). Validação por magic number, limite de tamanho configurável, URL pré-assinada | Claude | ✅ |
| F01-036 | TEC | Port `ProductImageStorage` + adapter S3 (AWS SDK v2, cadeia padrão de credenciais) + Resilience4j + testes com LocalStack via Testcontainers | Claude | ✅ |
| F01-037 | TEC | Script AWS CLI em `infra/aws/` para o perfil `dev`: bucket privado (Block Public Access), ciclo de vida só para uploads multipart incompletos (sem expiração de objetos: apagaria imagens válidas; órfãos → IF-026), política IAM restrita ao bucket. **Você executa.** | Claude | 🔄 |
| F01-038 | ESTUDO | Configurar **alerta de orçamento** (AWS Budgets) e usuário IAM sem root antes de qualquer uso da AWS | Você | ⬜ |
| F01-039 | TEC | Catálogo de erros do módulo (`PRODUCT_NOT_FOUND`, `INSUFFICIENT_STOCK`, `INVALID_IMAGE_TYPE`, …) | Claude | ✅ |

---

## Marco 01.4 — Vendas

Módulo `order` e `payment` do `sales-service`

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-040 | FEAT | **Spec: Criar pedido** — cliente validado via REST, preço do servidor, snapshot de itens e endereço, `Idempotency-Key` | Ambos | ⬜ |
| F01-041 | TEC | Migrations: `orders` (sequence de `order_number`, `CHECK` do total), `order_items` (coluna gerada `line_total`), `order_status_history`, `payments` (um aprovado por pedido) | Claude | ⬜ |
| F01-042 | FEAT | Criar pedido: validação do cliente, preço e total calculados no servidor, **baixa de estoque atômica travando produtos em ordem de ID** (evita deadlock), snapshot, idempotência | Claude | ⬜ |
| F01-043 | TEC | `CustomerClient` (HTTP Interface) com **Circuit Breaker, Retry com backoff, TimeLimiter** e fallback (503 em `ProblemDetail`) | Claude | ⬜ |
| F01-044 | TEC | **Teste de contrato (consumidor)** contra o stub gerado pelo customer-service | Claude | ⬜ |
| F01-045 | FEAT | Máquina de estados do pedido (padrão **State**): `CREATED → AWAITING_PAYMENT → PAID → SHIPPED → DELIVERED` / `CANCELLED`, com histórico | Claude | ⬜ |
| F01-046 | FEAT | Pagamento simulado (padrão **Strategy**): PIX, cartão, boleto; tentativas múltiplas; regra fixa de aprovação/recusa | Claude | ⬜ |
| F01-047 | FEAT | Cancelamento de pedido com **devolução de estoque** na mesma transação | Claude | ⬜ |
| F01-048 | FEAT | Consultas: pedido por ID; pedidos por cliente (paginado); ações administrativas de envio e entrega | Claude | ⬜ |
| F01-049 | TEC | **Testes de concorrência com Virtual Threads**: 100 pedidos simultâneos para o último item; pedidos cruzados com vários itens (deadlock) | Claude | ⬜ |
| F01-050 | ESTUDO | Derrubar o customer-service com o sales-service rodando e observar o Circuit Breaker (estados, métricas no Actuator) | Você | ⬜ |

---

## Marco 01.5 — BFF

`services/web-bff` · sem banco de dados · stateless

**Regra de ouro:** o BFF **só compõe e adapta** dados para as telas. Nenhuma regra de negócio.

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-051 | ADR | Papel do BFF e divisão de responsabilidades com o futuro API Gateway (borda × experiência da interface) | Claude | ⬜ |
| F01-052 | TEC | Esqueleto do `web-bff` (mesma base do F01-012: Virtual Threads, Actuator, logs JSON, ID de correlação, `ProblemDetail`, OpenAPI, ArchUnit); rule `bff.md` | Claude | ⬜ |
| F01-053 | TEC | Clientes HTTP Interface para `customer-service` e `sales-service` com Resilience4j | Claude | ⬜ |
| F01-054 | TEC | **Testes de contrato (consumidor)** do BFF contra os stubs dos dois serviços | Claude | ⬜ |
| F01-055 | FEAT | **Spec: contrato das telas** — endpoints da loja (`/store/**`) e do admin (`/admin/**`) desenhados a partir das telas, não das APIs internas | Ambos | ⬜ |
| F01-056 | FEAT | Endpoints da loja: catálogo, detalhe do produto, identificação do cliente, checkout, meus pedidos, cancelamento | Claude | ⬜ |
| F01-057 | FEAT | Endpoints do admin: clientes, categorias, produtos, estoque, imagens (repasse do multipart), pedidos com dados do cliente | Claude | ⬜ |
| F01-058 | TEC | **Agregação em paralelo com `StructuredTaskScope`** (Structured Concurrency) e **degradação parcial** quando um serviço falha | Claude | ⬜ |
| F01-059 | ESTUDO | Derrubar cada serviço e observar como as telas do BFF degradam | Você | ⬜ |

---

## Marco 01.6 — Frontend

`web/aishac-shop-web`

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-060 | TEC | Setup: Vite + React + TypeScript, Tailwind, Lucide, ESLint/Prettier, Vitest + Testing Library; **workflow de CI do frontend** (padrão do F01-014) | Claude | ⬜ |
| F01-061 | TEC | Cliente gerado a partir do **OpenAPI do BFF** (o frontend não conhece os serviços) + Axios (interceptor de ID de correlação e `ProblemDetail`) + TanStack Query | Claude | ⬜ |
| F01-062 | FEAT | Loja: listagem com filtros e paginação, detalhe do produto com galeria | Claude | ⬜ |
| F01-063 | FEAT | Carrinho com Zustand, persistido no navegador | Claude | ⬜ |
| F01-064 | FEAT | Checkout: identificação do cliente (sem login: busca/cadastro por e-mail ou CPF), endereço, forma de pagamento, envio com `Idempotency-Key` | Claude | ⬜ |
| F01-065 | FEAT | Meus pedidos (do cliente identificado) e cancelamento | Claude | ⬜ |
| F01-066 | FEAT | `/admin`: clientes, categorias, produtos, estoque, upload e gestão de imagens, envio/entrega de pedidos | Claude | ⬜ |
| F01-067 | TEC | Tratamento de erros e estados de carregamento padronizados; formulários com React Hook Form + Zod | Claude | ⬜ |
| F01-068 | TEC | **E2E com Playwright**: fluxo de compra completo e cadastro de produto com imagem | Claude | ⬜ |

---

## Marco 01.7 — Fechamento

| ID | Tipo | Item | Dono | Status |
|---|---|---|---|---|
| F01-070 | TEC | Teste de carga com **k6** pelo BFF: criação de pedidos e telas agregadas, com e sem Virtual Threads | Claude | ⬜ |
| F01-071 | ESTUDO | Executar o teste de carga e registrar os resultados | Você | ⬜ |
| F01-072 | TEC | Revisão final pelo `code-reviewer` e ArchUnit em todo o código | Claude | ⬜ |
| F01-073 | DOC | Atualizar diagrama da fase e ADRs com o que mudou durante a construção | Claude | ⬜ |
| F01-074 | ESTUDO | **Preencher `limitacoes.md`** com o que você observou na arquitetura síncrona | Você | ⬜ |
| F01-075 | TEC | Criar a tag `fase-01-base-sincrona` | Você | ⬜ |

---

## Itens opcionais de aprofundamento na F01

| ID | Tipo | Item |
|---|---|---|
| F01-090 | TEC | Separar usuário de migração (DDL, Flyway) do usuário da aplicação (DML) |
| F01-091 | TEC | Testes de mutação (PIT) no domínio |
| F01-092 | TEC | Espelhar o backlog no GitHub Issues/Projects |
