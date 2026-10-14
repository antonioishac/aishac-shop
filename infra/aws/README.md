# Scripts AWS CLI

Infraestrutura na **AWS real**, usada só pelo perfil `dev`. Os scripts são **executados apenas por você** (premissa P3): o Claude escreve e revisa, mas não roda a AWS CLI (o hook `guard-bash` bloqueia).

## Antes de tudo (F01-038)
1. Alerta de orçamento no AWS Budgets.
2. Usuário IAM sem root, com um perfil na AWS CLI (`aws configure --profile aishac-dev`).
3. AWS CLI v2 e Git Bash (no Windows).

## Bucket de imagens de produto (F01-037)

| Script | O que faz |
|---|---|
| `dev/product-images-bucket.sh` | Cria (ou reaplica) o bucket privado em `sa-east-1` e a política IAM `aishac-sales-service-product-images-dev`. Idempotente. |
| `dev/product-images-bucket-remove.sh` | Apaga o bucket com **todos** os objetos e a política. Irreversível; pede o nome do bucket para confirmar. |

```bash
export AWS_PROFILE=aishac-dev
export PRODUCT_IMAGES_BUCKET=aishac-product-images-dev-<sufixo-unico>   # nome global no S3
export AWS_IAM_USER=aishac-dev                                          # opcional: anexa a política
./infra/aws/dev/product-images-bucket.sh
```

Atalhos: `task aws:bucket` e `task aws:bucket:remove` (mesmas variáveis).

O que o bucket recebe:
- **Block Public Access** completo e `BucketOwnerEnforced` (sem ACL): a entrega é só por URL pré-assinada (ADR-010).
- Criptografia padrão **SSE-S3**.
- Ciclo de vida **só** para abortar uploads multipart incompletos (1 dia). **Não há expiração de objetos:** o S3 não distingue um objeto órfão de uma imagem válida, e uma regra de expiração no bucket apagaria as imagens dos produtos. Órfãos (exclusão no S3 que falhou depois de o banco excluir) são uma limitação aceita da fase 01: ficam no log de aviso e na métrica `aishac.storage.orphaned.objects`; a limpeza é a ideia IF-026.
- Política IAM de mínimo privilégio: `PutObject`, `GetObject` e `DeleteObject` em `products/*` e `ListBucket` restrito ao prefixo `products/`.

Para o sales-service no perfil `dev`: `PRODUCT_IMAGES_BUCKET=<nome>` e as credenciais pela cadeia padrão do SDK (`AWS_PROFILE`). Nada de chave no código ou no `config-repo`.
