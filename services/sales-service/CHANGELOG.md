# Changelog — sales-service

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento semântico por serviço (ver `CONTRIBUTING.md`).

## [Unreleased]

### Adicionado
- Catálogo (F01-030 a F01-036, F01-039), módulo `catalog`:
  - categorias com nome único sem diferenciar maiúsculas, ativação e desativação;
  - produtos com SKU único em maiúsculas, preço como string decimal, lock otimista e filtro `sellable` (produto e categoria ativos);
  - estoque com entrada e saída atômicas (`UPDATE ... RETURNING`) e histórico append-only;
  - imagens no S3 (até 8 por produto): tipo validado pelo magic number, URL pré-assinada, substituição com chave nova, compensação se o banco falhar e Circuit Breaker `s3` (`503 IMAGE_STORAGE_UNAVAILABLE`); objeto que fica órfão numa exclusão incrementa a métrica `aishac.storage.orphaned.objects`.
- Migrations `V1` a `V4` (categorias, produtos, movimentações de estoque e imagens) e seed `R__seed_catalog` só no perfil `local`.
- Parâmetro ou parte multipart obrigatória ausente responde `400 VALIDATION_ERROR` com o campo; arquivo acima do limite do multipart (10 MB) responde `400 FILE_TOO_LARGE`.
- Esqueleto do serviço: configuração pelo Config Server, logs JSON com máscara de CPF/e-mail, ID de correlação (com propagação nas chamadas de saída), ProblemDetail, versionamento de API, OpenAPI, Dockerfile e gates de qualidade.
