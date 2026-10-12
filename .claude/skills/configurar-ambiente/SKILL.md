---
name: configurar-ambiente
description: Adiciona ou altera propriedades de configuração de um serviço no config-repo para os perfis local, dev e prod de uma só vez, com @ConfigurationProperties tipado e sem segredos versionados. Use sempre que uma configuração nova surgir.
argument-hint: "[servico] [propriedade]"
---

# Configurar ambiente

Siga a rule `configuration`.

1. Decida o escopo: comum a todos (`config-repo/application.properties`), comum ao serviço (`<servico>.properties`) ou varia por perfil (`<servico>-local|dev|prod.properties`).
2. Se varia por perfil, defina nos **três** arquivos. Valores de referência:
   | Perfil | Banco | S3 | Logs |
   |---|---|---|---|
   | local | container do usuário | LocalStack | DEBUG para `com.aishac` |
   | dev | container do usuário | AWS `sa-east-1` | INFO |
   | prod | (estudo) | AWS | INFO, JSON |
3. Segredo? Use `${NOME_DA_VARIAVEL}` e acrescente a variável ao `infra/docker/.env.example` **sem valor real**, com comentário.
4. Propriedade própria (`aishac.*`) → Record com `@ConfigurationProperties` + `@Validated`, registrado com `@ConfigurationPropertiesScan`.
5. Mostre ao usuário uma tabela propriedade × perfil com os valores definidos.
