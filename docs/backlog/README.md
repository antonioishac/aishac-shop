# Aishac Shop — Backlog de Estudo

Backlog mestre do projeto **Aishac Shop**: um e-commerce construído como estudo de ponta a ponta, do código à nuvem.
Este diretório é a **fonte da verdade** do que será feito. O Claude Code lê estes arquivos para saber o que vem a seguir.

## Índice

| Arquivo | Conteúdo |
|---|---|
| [fase-01-base-sincrona.md](fase-01-base-sincrona.md) | Fase atual: a base sólida, 100% síncrona, dividida em 7 marcos |
| [trilha-aplicacao.md](trilha-aplicacao.md) | Fases 02 a 04: API Gateway, Autenticação, Eventos com Kafka |
| [trilha-cloud.md](trilha-cloud.md) | Fases C1 a C5: Terraform, IAM, ECR, RDS, EKS, deploy contínuo |
| [ideias-futuras.md](ideias-futuras.md) | Ideias registradas que ainda não têm fase |

## Trilhas de estudo

```text
Trilha Aplicação:  F01 Base síncrona ─▶ F02 API Gateway ─▶ F03 Autenticação ─▶ F04 Eventos (Kafka)
                        │
Trilha Cloud:           └──▶ C1 Terraform+IAM ─▶ C2 ECR ─▶ C3 Rede+RDS ─▶ C4 EKS ─▶ C5 Deploy contínuo
```

- A **F01 é pré-requisito de tudo**.
- A trilha Cloud pode começar logo após a F01 e evoluir em paralelo com a trilha Aplicação.
- Cada fase fecha com: diagrama draw.io da fase, ADRs, `limitacoes.md`, `de-para.md` (a partir da F02) e **tag git**.

## Convenções do backlog

**IDs:** `F01-001` (fase de aplicação), `C1-001` (fase cloud), `IF-001` (ideia futura).

**Tipos:**
| Tipo | Significado |
|---|---|
| `FEAT` | Funcionalidade de negócio (passa pelo ciclo SDD completo) |
| `TEC` | Item técnico (infraestrutura, configuração, qualidade) |
| `ADR` | Decisão de arquitetura a registrar |
| `DOC` | Documentação / diagrama |
| `ESTUDO` | Atividade de estudo e observação (feita por você) |

**Status:** `⬜ A fazer` · `🔄 Em andamento` · `✅ Concluído` · `⏸️ Bloqueado` · `🚫 Descartado`

**Dono:** `Claude` (gera o artefato), `Você` (executa, decide ou estuda), `Ambos`.

## Definition of Ready (DoR) — antes de começar um `FEAT`
1. Spec (`specs/<servico>/<id>-<nome>/spec.md`) escrita e **aprovada por você**.
2. Contrato da API (OpenAPI) definido.
3. Modelo de dados da feature **aprovado** (portão próprio, antes da migration).
4. Critérios de aceite em Given/When/Then.
5. Dependências do item concluídas.

## Definition of Done (DoD)
1. Código segue as rules (`.claude/rules/`) e passa no ArchUnit.
2. Testes unitários, de fatia e de integração (Testcontainers) passando; cobertura mínima atingida.
3. Endpoints documentados (OpenAPI) e coleção `.http` atualizada.
4. Revisão do `code-reviewer` (contexto limpo) sem pendências.
5. Diagrama e ADRs atualizados quando a arquitetura mudou.
6. CI verde. Commit semântico.

---

## Premissas do projeto (inegociáveis)

| # | Premissa |
|---|---|
| P1 | **Você controla os containers.** O Claude escreve os `docker-compose` e informa os comandos; nunca executa `docker` ou `docker compose`. Exceção: o **Testcontainers** pode rodar nos testes. |
| P2 | **Credenciais são suas.** O Claude nunca conhece nem lê senhas. `.env` fora do Git e bloqueado para leitura pelo Claude. |
| P3 | **Infraestrutura paga é decisão sua.** O Claude roda `terraform plan`; `apply` e `destroy` são seus. |
| P4 | **Base sólida, não base com tudo pronto.** Flexibilidade nos pontos de mudança previsíveis; nada antecipado de fases futuras. |
| P5 | **O estudo vem antes da solução.** Limitações são observadas e registradas por você antes de serem resolvidas na fase seguinte. |

## Registro de decisões consolidadas

Decisões tomadas na concepção do projeto. Cada uma vira um ADR formal no marco 01.1 (item F01-008).

| # | Decisão |
|---|---|
| D01 | **Monorepo** com build independente por serviço (Maven + Wrapper em cada um). Escala e deploy individuais por serviço. Sem biblioteca compartilhada de domínio. |
| D02 | **Java 25**, **Spring Boot 4.1.x**, **Spring Cloud 2025.1.x (Oakwood)**. Versões de patch confirmadas na criação. |
| D03 | **Lombok mantido**, com `lombok-mapstruct-binding` e regras de uso (`@RequiredArgsConstructor`, `@Slf4j`, `@Getter`; proibido `@Data`/`@EqualsAndHashCode` em entidade JPA; Records para DTOs). |
| D04 | ~~Arquitetura hexagonal leve, organizada por feature.~~ **Revista:** camadas por tipo (controller, service, repository...), ADR-025. |
| D05 | **Dois microserviços:** `customer-service` e `sales-service`. **Catálogo** é um módulo do `sales-service` com fronteira de pacote rígida. |
| D06 | **Fase 01 100% síncrona.** Sem broker, sem Outbox, sem eventos internos do Spring. Diagrama também síncrono. |
| D07 | **Um banco por serviço** (`aishac_customer`, `aishac_sales`). Localmente, um único container PostgreSQL com os dois bancos e **um usuário por serviço** sem acesso cruzado. |
| D08 | Convenções de banco: UUID v7, `NUMERIC(19,2)` para dinheiro, `TIMESTAMPTZ`, status em `VARCHAR + CHECK`, `version` para lock otimista, constraints nomeadas. |
| D09 | Erros em **`ProblemDetail` (RFC 9457)** com catálogo de códigos de erro. |
| D10 | **Resilience4j** nas chamadas Vendas → Clientes e no S3 (ADR comparando com o retry nativo do Spring 7). |
| D11 | **Imagens no S3:** upload via backend (multipart), bucket privado, URL pré-assinada, banco guarda a chave (não a URL), substituição grava chave nova, exclusão física (banco primeiro, S3 depois). |
| D12 | **LocalStack** (token Hobby) no ambiente local e nos testes; **AWS real** só no perfil `dev`. |
| D13 | **Spring Cloud Config Server** com `config-repo/`, arquivos `.properties`, perfis `local`, `dev` e `prod` (este só como configuração). Nenhum segredo versionado. |
| D14 | **Frontend:** React + Vite + **TypeScript**, Tailwind, Lucide, Zustand (carrinho e sessão), **TanStack Query** (dados do servidor), Axios com **cliente gerado do OpenAPI**, React Hook Form + Zod. |
| D15 | Carrinho **só no navegador**; pagamento **simulado** (Strategy); frete fixo por configuração. |
| D16 | Código em **inglês**; documentação e specs em **português**. |
| D17 | **O servidor é a autoridade sobre preço e total.** Frontend envia só produto e quantidade. |
| D18 | Cliente com pedidos **não é excluído**, apenas desativado (LGPD/histórico). |
| D19 | Dinheiro na API como **string decimal** (`"129.90"`); `BigDecimal` com arredondamento definido; moeda BRL. |
| D20 | Criação de pedido com cabeçalho **`Idempotency-Key`**. |
| D21 | **MCP do PostgreSQL** apontando só para o seu container local, com usuário `claude_mcp` (dados sim, estrutura não), log de todos os comandos e aprovação para alterações. |
| D22 | **Sem Eureka:** descoberta por DNS (docker-compose; depois Kubernetes). |
| D23 | Diagrama **draw.io versionado por fase** em `docs/evolucao/<fase>/`. |
| D24 | API Gateway e Keycloak ficam para as fases 02 e 03. |
| D25 | Testes: Claude pode rodar `mvn test` e `mvn verify` (Testcontainers incluído). |
| D26 | **BFF (`web-bff`) já na fase 01.** O frontend conhece só o BFF. O BFF compõe e adapta dados para as telas, sem regra de negócio e sem banco. Um BFF para loja e `/admin`. Na F02, o Gateway fica na frente do BFF. |
