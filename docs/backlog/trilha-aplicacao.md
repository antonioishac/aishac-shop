# Trilha Aplicação — Fases 02 a 04

As fases são descritas pelo **objetivo de estudo** e pelos **temas a explorar**, não pela solução detalhada.
O desenho de cada fase será feito no início dela, a partir das limitações observadas na fase anterior (premissa P5).

Toda fase fecha com: diagrama da fase, ADRs, `limitacoes.md`, `de-para.md` e tag git.

---

## F02 — API Gateway

**Pré-requisito:** F01 concluída.
**Objetivo de estudo:** entender o papel de um ponto de entrada único entre o frontend e os microserviços.

| ID | Tipo | Tema |
|---|---|---|
| F02-001 | ADR | Escolha e posicionamento do gateway (Spring Cloud Gateway) |
| F02-002 | TEC | Novo serviço `api-gateway` na frente do `web-bff` (os serviços internos deixam de ser expostos) |
| F02-003 | TEC | CORS, rate limit e timeouts centralizados na borda |
| F02-004 | TEC | Propagação do ID de correlação a partir do gateway |
| F02-005 | ESTUDO | Divisão de papéis Gateway × BFF na prática (revisitar o ADR F01-051) |
| F02-006 | TEC | Frontend passa a chamar o gateway, que encaminha ao BFF |
| F02-007 | DOC | Diagrama, `de-para.md` (F01 → F02), `limitacoes.md` |
| F02-008 | TEC | Tag `fase-02-api-gateway` |

---

## F03 — Autenticação

**Pré-requisito:** F02 concluída.
**Objetivo de estudo:** autenticação e autorização em microserviços com OAuth2/OIDC.

| ID | Tipo | Tema |
|---|---|---|
| F03-001 | ADR | Modelo de identidade (Keycloak, realms, clients, papéis `CUSTOMER` e `ADMIN`) |
| F03-002 | TEC | Keycloak no docker-compose (você executa) e configuração de realm versionada |
| F03-003 | TEC | Validação de JWT no gateway e nos serviços (resource servers) |
| F03-011 | TEC | **Padrão BFF para tokens**: o `web-bff` guarda os tokens no servidor e o navegador recebe só um cookie de sessão seguro |
| F03-004 | TEC | Coluna `auth_subject` em `customers` ligando cliente ao usuário autenticado |
| F03-005 | FEAT | Login, logout e cadastro no frontend; "Meus pedidos" passa a usar o usuário logado |
| F03-006 | FEAT | Proteção do `/admin` por papel |
| F03-007 | TEC | Autorização por recurso (cliente só vê os próprios pedidos) |
| F03-008 | TEC | Comunicação serviço a serviço autenticada (Vendas → Clientes) |
| F03-009 | DOC | Diagrama, `de-para.md` (F02 → F03), `limitacoes.md` |
| F03-010 | TEC | Tag `fase-03-autenticacao` |

---

## F04 — Eventos com Kafka

**Pré-requisito:** F01 concluída (recomendado após F03, para estudar sobre a base completa).
**Objetivo de estudo:** arquitetura orientada a eventos — ponto central do projeto.

| ID | Tipo | Tema |
|---|---|---|
| F04-001 | ESTUDO | Revisar o `limitacoes.md` das fases anteriores e escolher quais limitações os eventos devem resolver |
| F04-002 | ADR | Kafka no projeto: tópicos, partições, chaves, formato e versionamento de eventos |
| F04-003 | TEC | Kafka no docker-compose (você executa) e ferramenta de visualização de tópicos |
| F04-004 | TEC | Publicação confiável de eventos (estudo do padrão Transactional Outbox) |
| F04-005 | TEC | Consumo idempotente e tratamento de mensagens duplicadas |
| F04-006 | TEC | Réplica local de dados de cliente no sales-service alimentada por eventos |
| F04-007 | TEC | Retentativas, dead letter topic e reprocessamento |
| F04-008 | TEC | Estudo de Saga (orquestração vs coreografia) |
| F04-009 | TEC | Schema Registry e evolução de contratos de eventos |
| F04-010 | TEC | Testes com Kafka via Testcontainers |
| F04-011 | DOC | Diagrama `v2` e **de-para completo** com a arquitetura síncrona da F01 (sequência "Criar Pedido", ER, containers) |
| F04-012 | TEC | Tag `fase-04-eventos-kafka` |
