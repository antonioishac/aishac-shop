# Ideias futuras

Ideias que surgiram durante a concepção e ainda não têm fase definida.
Quando uma ideia ganhar fase, ela recebe ID definitivo no arquivo da trilha e é marcada aqui como `→ movida`.

## Produto

| ID | Ideia | Observação |
|---|---|---|
| IF-001 | Carrinho no servidor | Hoje o carrinho vive só no navegador (D15) |
| IF-002 | Cupons de desconto | Hoje o desconto existe no modelo, sem regra |
| IF-003 | Cálculo de frete real | Hoje o frete é fixo por configuração |
| IF-004 | Gateway de pagamento real (sandbox) | Hoje o pagamento é simulado |
| IF-005 | Anonimização de cliente (LGPD) | Hoje o cliente só é desativado (D18) |
| IF-006 | Categorias em vários níveis | Hoje são de um nível |
| IF-007 | Redimensionamento/miniaturas de imagens | Hoje a imagem é servida no tamanho original |

## Arquitetura

| ID | Ideia | Observação |
|---|---|---|
| IF-010 | Extrair o catálogo para um `catalog-service` (padrão Strangler) | Hoje é módulo do sales-service (D05) |
| IF-011 | Cache (ex.: Redis) para o catálogo | |
| IF-012 | CDN para as imagens | |
| IF-013 | Separar o BFF em `store-bff` e `admin-bff` | Hoje é um BFF com grupos de rotas (D26) |
| IF-014 | App mobile com BFF próprio atrás do mesmo Gateway | |

## Plataforma e operação

| ID | Ideia | Observação |
|---|---|---|
| IF-020 | Observabilidade completa: Prometheus, Grafana, Tempo, Loki | Na F01 há só Actuator, logs JSON e ID de correlação |
| IF-021 | Cofre de segredos (AWS Secrets Manager integrado ao Config Server, ou Vault) | |
| IF-022 | Kafka gerenciado na nuvem (MSK) | Após F04 e C4 |
| IF-023 | Infraestrutura local com Kubernetes (kind/k3d) antes do EKS | Estudo sem custo |
| IF-024 | Alternativa ao LocalStack sem conta (ex.: Floci) | Avaliar se o token virar problema |
| IF-025 | Busca de cliente por CPF/e-mail sem dado pessoal na URL (ex.: `POST /customers/search`) | Hoje `GET /customers?cpf=&email=` pode deixar CPF e e-mail em logs de acesso e de proxy. Avaliar com o Gateway (F02) |
| IF-026 | Limpeza de objetos órfãos do S3: job de reconciliação banco × bucket (apaga objetos de `products/` sem registro há mais de 1 dia) | Na F01 os órfãos são aceitos e observados pela métrica `aishac.storage.orphaned.objects` (ADR-010). Avaliar com eventos (F04): exclusão confirmada → remoção com retry |

## Qualidade e processo

| ID | Ideia | Observação |
|---|---|---|
| IF-030 | Testes de mutação (PIT) | Também listado como opcional na F01 (F01-091) |
| IF-031 | SonarQube | |
| IF-032 | Backlog no GitHub Issues/Projects | Também opcional na F01 (F01-092) |
