# ADR-011 — LocalStack no local e nos testes; AWS real só no perfil dev

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D12 · F01-010 · F01-018 · F01-036 |

## Contexto
O S3 é a única dependência AWS da fase 01. Desenvolver e testar contra a AWS real tem custo, exige credenciais e torna os testes dependentes de rede. Desde a versão 2026.03, a imagem do LocalStack é única e **exige token de autenticação** (o plano Hobby é gratuito para uso não comercial).

## Decisão
- **Perfil `local` e testes:** LocalStack (`localstack/localstack:2026.03.0`, versão fixada) com `LOCALSTACK_AUTH_TOKEN` vindo do ambiente do usuário. O bucket é criado por script de inicialização, e o healthcheck só passa quando o bucket existe. Nos `*IT`, o LocalStack sobe pelo Testcontainers.
- **Perfil `dev`:** AWS real (`sa-east-1`), com bucket criado por script AWS CLI executado pelo usuário (F01-037), credenciais pela cadeia padrão do SDK e alerta de orçamento antes de qualquer uso (F01-038).
- **Perfil `prod`:** somente configuração de estudo.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| AWS real em todos os ambientes | Fidelidade total | Custo, credenciais e rede nos testes |
| MinIO | Sem conta nem token | Compatível só com a API do S3; não cresce para outros serviços AWS das fases cloud |
| **LocalStack (Hobby)** | Emula vários serviços AWS; integra com Testcontainers | Exige token; a imagem pode mudar de licença outra vez |

## Consequências
- **Positivas:** testes de S3 reprodutíveis e sem custo; o mesmo código roda contra LocalStack e AWS, mudando só a configuração.
- **Negativas / custos:** o token vira pré-requisito do ambiente (no CI também, quando os testes de S3 existirem); emulação não é 100% fiel.
- **O que observar:** mudanças de licença do LocalStack (alternativa registrada em IF-024); diferenças de comportamento entre LocalStack e AWS.
