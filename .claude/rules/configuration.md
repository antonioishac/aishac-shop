---
paths:
  - "config-repo/**"
  - "services/**/src/main/resources/**"
  - "services/config-server/**"
---

# Configuração

- Cada serviço mantém localmente apenas `spring.application.name` e `spring.config.import=configserver:${CONFIG_SERVER_URL:http://localhost:8888}`, com `spring.cloud.config.fail-fast=true` e retry.
- Todo o resto vive em `config-repo/`, em `.properties`:
  - `application.properties` → comum a todos os serviços;
  - `<servico>.properties` → comum aos perfis do serviço;
  - `<servico>-<perfil>.properties` → `local`, `dev`, `prod`.
- **Segredos nunca versionados.** Use `${DB_PASSWORD}`, `${AWS_REGION}` etc., preenchidos por variáveis de ambiente.
- Propriedades próprias com prefixo `aishac.` e mapeadas em `@ConfigurationProperties` (Records).
- Ao adicionar uma propriedade que varia por ambiente, defina-a nos três perfis (skill `configurar-ambiente`).
- Valores do perfil `prod` são configuração de estudo: não existe ambiente implantado.
