---
paths:
  - "services/sales-service/**/catalog/**"
  - "infra/aws/**"
---

# Armazenamento de imagens (S3)

- Acesso pela interface `ProductImageStorage` (em `service/`), implementada com **AWS SDK for Java v2** e **cadeia padrão de credenciais**. Nunca chaves no código, em `.properties` ou no `config-repo`.
- Endpoint e bucket configuráveis: `local` → LocalStack; `dev` → AWS (`sa-east-1`).
- Bucket **privado** (Block Public Access). Entrega por **URL pré-assinada** com expiração curta (configurável, padrão 15 min).
- O banco guarda a **chave do objeto** (`products/{productId}/{uuid}.{ext}`, UUID novo a cada arquivo), nunca a URL.
- Upload via backend (multipart): validar tipo pelo **magic number** (JPEG, PNG, WebP), não pelo `Content-Type` enviado; limite de tamanho configurável (padrão 5 MB).
- **Inserir:** grava no S3 → grava no banco; se o banco falhar, remove o objeto (compensação).
- **Substituir:** grava em **chave nova** → atualiza o banco → remove o objeto antigo. Nunca sobrescreve a mesma chave.
- **Excluir:** remove do banco → remove do S3. Falha no S3 deixa órfão (aceitável na fase 01: log de aviso + métrica `aishac.storage.orphaned.objects`). **Nunca** criar regra de expiração de objetos no bucket: apagaria imagens válidas.
- Chamadas ao S3 com Circuit Breaker do Resilience4j (instância `s3`); retry e timeouts ficam com o próprio AWS SDK, para não multiplicar retentativas. Falha ou circuito aberto → `503 IMAGE_STORAGE_UNAVAILABLE`.
- Scripts em `infra/aws/` são **executados pelo usuário**; política IAM restrita ao bucket (`s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, `s3:ListBucket`).
