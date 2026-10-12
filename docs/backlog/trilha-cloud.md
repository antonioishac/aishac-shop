# Trilha Cloud — Fases C1 a C5

**Pré-requisito:** F01 concluída.
**Objetivo de estudo:** levar os serviços para a AWS com infraestrutura como código, escalável e configurável.

## Regras desta trilha

- **Ambientes efêmeros:** `terraform apply` no início da sessão de estudo, `terraform destroy` no fim. EKS, NAT Gateway, Load Balancer e RDS geram custo por hora.
- **O Claude roda `terraform plan`; `apply` e `destroy` são seus** (premissa P3, bloqueado nas permissões).
- **IAM é transversal:** cada fase acrescenta permissões mínimas, por serviço.
- **Versão do Kubernetes fixada** e política de upgrade explícita, para evitar a cobrança de suporte estendido do EKS.
- Ao iniciar a trilha, entram no Claude Code: agentes `terraform-engineer` e `platform-k8s-engineer`, skills `terraform-modulo` e `helm-chart-servico`, rule `terraform.md`.

## Estrutura alvo do Terraform

```text
infra/terraform/
├── bootstrap/        # bucket do estado remoto (criado uma única vez)
├── modules/          # network, ecr, rds, eks, s3, iam
└── environments/
    ├── dev/          # main.tf + dev.tfvars
    └── prod/         # main.tf + prod.tfvars
```

Tudo parametrizado (tamanhos, réplicas, nós, versões), com tags padrão em todo recurso.

---

## C1 — Fundamentos: Terraform + IAM

| ID | Tipo | Tema |
|---|---|---|
| C1-001 | ESTUDO | Alerta de orçamento, MFA na conta root, usuário/role administrativa sem root |
| C1-002 | TEC | Bootstrap do estado remoto (S3 com lock) |
| C1-003 | TEC | Estrutura de módulos e ambientes `dev`/`prod` |
| C1-004 | TEC | Módulo `s3`: bucket de imagens migrado do script AWS CLI da F01 para Terraform |
| C1-005 | TEC | Módulo `iam`: política mínima por serviço |
| C1-006 | TEC | Rule `terraform.md`, gates (`fmt`, `validate`, `tflint`, varredura de segurança) no CI |
| C1-007 | DOC | Diagrama e ADRs da fase |

## C2 — Contêineres + ECR

| ID | Tipo | Tema |
|---|---|---|
| C2-001 | TEC | Módulo `ecr`: um repositório por serviço |
| C2-002 | TEC | Varredura de vulnerabilidades e política de retenção de imagens |
| C2-003 | TEC | Revisão das imagens (multi-stage, usuário não root, tamanho) |
| C2-004 | ESTUDO | Publicar as imagens manualmente no ECR |

## C3 — Rede + RDS

| ID | Tipo | Tema |
|---|---|---|
| C3-001 | TEC | Módulo `network`: VPC, subnets públicas e privadas, NAT |
| C3-002 | TEC | Módulo `rds`: PostgreSQL privado (de-para: 1 container com 2 bancos → instâncias/bancos na nuvem) |
| C3-003 | TEC | Senhas no Secrets Manager e acesso por IAM |
| C3-004 | TEC | Flyway rodando contra o RDS |
| C3-005 | ESTUDO | Custos de rede (NAT) e alternativas |

## C4 — EKS

| ID | Tipo | Tema |
|---|---|---|
| C4-001 | TEC | Módulo `eks`: cluster, node groups, versão fixada |
| C4-002 | TEC | Helm chart por serviço (probes, recursos, configuração por ambiente) |
| C4-003 | TEC | **HPA**: escala individual do `sales-service` |
| C4-004 | TEC | Ingress com Load Balancer |
| C4-005 | TEC | Acesso ao S3 e ao Secrets Manager por **identidade do pod** (sem chaves) |
| C4-006 | ADR | Config Server no Kubernetes vs ConfigMaps/Secrets (de-para) |
| C4-007 | ESTUDO | Teste de carga (k6) observando o HPA escalar |

## C5 — Deploy contínuo

| ID | Tipo | Tema |
|---|---|---|
| C5-001 | TEC | Autenticação do GitHub Actions na AWS via **OIDC** (sem chave salva) |
| C5-002 | TEC | Pipeline: build → push no ECR → deploy no EKS por serviço |
| C5-003 | TEC | Estratégia de rollout e rollback |
| C5-004 | TEC | Promoção entre `dev` e `prod` |
| C5-005 | DOC | Diagrama final da trilha e `de-para.md` |
