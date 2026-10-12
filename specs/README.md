# Specs (Spec-Driven Development)

Uma pasta por funcionalidade, criada pela skill `sdd-feature`:

```text
specs/<contexto>/<ID-do-backlog>-<nome>/
├── spec.md        # o quê e por quê: regras, critérios de aceite, modelo de dados
├── contract.yaml  # contrato OpenAPI da feature
├── plan.md        # como: arquivos, padrões, testes
└── tasks.md       # passos executáveis com status
```

Contextos: `customer`, `sales`, `bff`, `web`.

Portões de aprovação do usuário: **spec.md** (antes do modelo de dados) e **modelo de dados** (antes da migration).
