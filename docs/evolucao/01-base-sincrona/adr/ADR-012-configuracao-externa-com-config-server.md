# ADR-012 — Configuração externa com Spring Cloud Config Server

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D13 · F01-011 · F01-012 · C4-006 |

## Contexto
Os serviços precisam de configuração diferente por ambiente (`local`, `dev`, `prod`) sem rebuild, sem segredos versionados e com um único lugar para consultar o que cada serviço recebe. O Spring Cloud Config Server não é distribuído como produto pronto: é uma biblioteca (`@EnableConfigServer`) embutida numa aplicação Spring Boot. A única imagem oficial (`springcloud/configserver`) está abandonada desde 2020.

## Decisão
- Um serviço próprio, **`config-server`** (uma classe + propriedades), servindo `config-repo/` em arquivos `.properties`, do mais geral ao mais específico: `application` → `application-<perfil>` → `<servico>` → `<servico>-<perfil>`.
- **Backend escolhido pelo perfil do servidor:** `native` (padrão, lê o disco; local) e `git` (`config-repo/` da branch `main` no GitHub; dev/prod).
- Os serviços guardam localmente só `spring.application.name` e `spring.config.import=configserver:...`, com **`fail-fast` e retry**.
- **Segredos só como `${VAR}`.** O `ConfigRepoSecretsTest` reprova valores literais em chaves sensíveis; o `ConfigServerIT` confere cada serviço × perfil.
- Os testes dos serviços carregam os arquivos reais do `config-repo` (perfil `local`) sem subir o Config Server.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Só variáveis de ambiente / `application-<perfil>.properties` no jar | Nada a operar | Configuração espalhada; mudar exige rebuild ou redeploy |
| Imagem de terceiro (`hyness/spring-cloud-config-server`) | Sem projeto próprio | Mantenedor externo; Java 17; fora do padrão da stack; sem testes nossos |
| ConfigMaps/Secrets do Kubernetes | Padrão em Kubernetes | Não existe Kubernetes na fase 01; comparação prevista no C4-006 |
| **Config Server próprio, native + git** | Forma oficial; versões da stack; backend Git como em produção | Um serviço a mais para subir e monitorar |

## Consequências
- **Positivas:** configuração centralizada e auditável; no backend `git`, a versão servida é o hash do commit.
- **Negativas / custos:** o Config Server vira dependência de partida de todos os serviços (mitigada por fail-fast + retry e pelo healthcheck no compose); no backend `git` só vale o que foi commitado e publicado.
- **O que observar:** tempo de subida dos serviços esperando o Config Server; a comparação com ConfigMaps/Secrets e cofres de segredo na trilha cloud (C4-006, IF-021).
