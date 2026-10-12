# ADR-019 — Sem Eureka: descoberta de serviços por DNS

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D22 · F01-010 · trilha cloud C4 |

## Contexto
Serviços precisam encontrar uns aos outros (sales → customer, BFF → serviços). O ecossistema Spring Cloud oferece o Eureka (registro de serviços + balanceamento no cliente), mas o destino do projeto é Kubernetes (EKS), que já resolve descoberta e balanceamento na plataforma.

## Decisão
**Não usar Eureka** nem outro registro de serviços. A descoberta é por **DNS da plataforma**:
- no Docker Compose, pelo nome do serviço na rede `aishac-net` (`http://customer-service:8081`);
- no Kubernetes, pelo `Service` (`http://customer-service.<namespace>.svc`).

As URLs entram por configuração (`config-repo`, variáveis de ambiente), nunca fixas no código.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Eureka + Spring Cloud LoadBalancer | Clássico no ecossistema Spring; balanceamento no cliente | Um servidor a mais para operar; redundante no Kubernetes |
| Consul | Descoberta + configuração + health | Outra peça de infraestrutura para estudar fora do foco |
| **DNS da plataforma** | Nada a operar; mesmo modelo no Compose e no Kubernetes | Sem balanceamento no cliente nem metadados de instância |

## Consequências
- **Positivas:** menos componentes; o código não sabe onde roda.
- **Negativas / custos:** no Compose há uma instância por serviço; com várias réplicas, o DNS devolve vários IPs e a JVM pode manter o resultado em cache.
- **O que observar:** distribuição desigual de carga com réplicas (cache de DNS da JVM); necessidade de *service mesh* nas fases cloud.
