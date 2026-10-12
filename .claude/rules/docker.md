---
paths:
  - "infra/docker/**"
  - "**/Dockerfile"
  - "**/compose*.yml"
---

# Docker

## Controle é do usuário
- O Claude **escreve** Dockerfiles e arquivos Compose e **informa** os comandos. **Nunca executa** `docker`, `docker compose` ou equivalentes (bloqueado nas permissões).
- Ao entregar um compose novo ou alterado, informe: o que mudou, o comando para subir, o comando para ver logs e o comando para derrubar.
- Exceção: Testcontainers dentro dos testes.

## Compose
- `compose.infra.yml` (PostgreSQL único com dois bancos, LocalStack) e `compose.apps.yml` (config-server, serviços, BFF, web).
- Variáveis vindas de `.env` (do usuário); o repositório só tem `.env.example` sem valores reais.
- Imagens com **versão fixada** (nunca `latest`).
- `healthcheck` em todos os serviços; `depends_on` com `condition: service_healthy`.
- Limites de memória definidos para caber numa máquina de desenvolvimento.
- Rede nomeada única; descoberta pelo nome do serviço (DNS do Compose).
- PostgreSQL com `log_statement` habilitado para o usuário `claude_mcp`, para o usuário auditar o que o Claude executa.

## Dockerfile (serviços Java)
- Multi-stage (build com Maven Wrapper → runtime JRE 25 enxuta).
- Usuário não root; `HEALTHCHECK` via Actuator; opções de JVM conscientes de container.
