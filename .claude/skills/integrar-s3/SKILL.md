---
name: integrar-s3
description: Implementa ou altera o armazenamento de imagens no S3 (port ProductImageStorage, adapter AWS SDK v2, URL pré-assinada, validação de arquivo, compensação, Resilience4j e testes com LocalStack). Use para qualquer trabalho com imagens de produto.
---

# Integração com S3

Siga a rule `storage`. Referências: [reference.md](reference.md).

## Componentes
1. **Port** (`catalog/application/port/out/ProductImageStorage`):
   `store(key, InputStream, contentType, size)`, `delete(key)`, `presignedUrl(key, Duration)`.
2. **Adapter** (`catalog/adapter/out/storage/S3ProductImageStorage`): `S3Client` + `S3Presigner` do AWS SDK v2, criados como beans com endpoint e região de `aishac.storage.*` (endpoint vazio = AWS real).
3. **Validação** (no caso de uso, antes do upload): tamanho ≤ limite; tipo pelo magic number.
4. **Casos de uso:** inserir, substituir (chave nova), atualizar metadados, excluir, listar/buscar com URL pré-assinada — com a ordem de operações e compensação da rule.
5. **Resiliência:** skill `configurar-resilience` com instância `s3`.
6. **Configuração** (skill `configurar-ambiente`): `aishac.storage.bucket`, `.region`, `.endpoint`, `.presigned-url-ttl`, `.max-file-size`, `.path-style-access` (true no LocalStack).
7. **Testes:** unitário da validação; `*IT` com LocalStack (upload, URL pré-assinada acessível, substituição remove o antigo, exclusão); falha no banco após upload remove o objeto.

## Infra do perfil `dev` (executada pelo usuário)
Atualize `infra/aws/` (script AWS CLI + política IAM) quando o bucket ou as permissões mudarem, e informe os comandos ao usuário. Nunca execute.
