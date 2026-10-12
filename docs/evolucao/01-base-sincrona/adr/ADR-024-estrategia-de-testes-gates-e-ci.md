# ADR-024 — Estratégia de testes, gates de qualidade e CI por serviço

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D25 · F01-013 · F01-014 · rule `testing` |

## Contexto
As regras de arquitetura, consistência e segurança só valem se forem verificadas automaticamente em cada mudança. O monorepo tem builds independentes (ADR-001), então a CI precisa rodar só o que mudou. O Claude Code pode rodar `./mvnw test` e `./mvnw verify` (Testcontainers incluído), mas não sobe containers fora dos testes.

## Decisão
**Pirâmide de testes:**
- muitos unitários (`*Test`, domínio sem Spring);
- testes de fatia (`@WebMvcTest`, `@DataJpaTest`);
- poucos de integração (`*IT`, `@SpringBootTest` + Testcontainers com a mesma versão do PostgreSQL do compose);
- contrato (Spring Cloud Contract) e E2E (Playwright).

**Gates no `./mvnw verify`:**
- **Spotless `check`** (Palantir Java Format);
- **ArchUnit** (ADR-004 e ADR-005);
- **JaCoCo** juntando unitários e `*IT`, com mínimo de **80% de linhas e 70% de desvios**.

**CI no GitHub Actions:**
- um workflow por serviço, **filtrado por caminho** (`services/<servico>/**` e `config-repo/**`), chamando o reutilizável `_java-service.yml`;
- o workflow `security.yml` roda **Gitleaks** (segredos no histórico) e **OSV-Scanner** (dependências vulneráveis; em PR, reprova só vulnerabilidade nova);
- **Dependabot** semanal para Maven, imagens Docker e Actions;
- localmente, o hook `on-stop` aplica o Spotless e roda `./mvnw test` nos serviços alterados.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| H2 nos testes de integração | Rápido; sem Docker | Comportamento diferente do PostgreSQL (locks, tipos, SQL) |
| OWASP Dependency-Check | Muito conhecido | Lento; exige chave da API do NVD (um segredo a mais) |
| Uma CI única para o monorepo | Simples | Tudo roda a cada mudança; contraria o build independente |
| **Testcontainers + gates no Maven + CI por caminho** | O mesmo comando local e na CI; feedback só do que mudou | Testes de integração mais lentos; Docker obrigatório |

## Consequências
- **Positivas:** uma violação de fronteira, uma queda de cobertura, um segredo ou uma vulnerabilidade nova bloqueiam o PR.
- **Negativas / custos:** `verify` mais lento; os mínimos de cobertura podem incentivar testes superficiais.
- **O que observar:** testes instáveis (Testcontainers, concorrência); cobertura de desvios perto do mínimo; tempo de CI crescendo. As actions estão fixadas por versão principal (`@v7`); fixar por SHA é um endurecimento possível.
