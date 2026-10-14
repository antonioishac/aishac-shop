#!/usr/bin/env bash
# F01-037 — Bucket de imagens de produto do perfil dev (AWS real, sa-east-1) e a política IAM do sales-service.
# Executado APENAS pelo usuário (premissa P3). Idempotente: rodar de novo reaplica as configurações.
#
# Uso (Git Bash), com um perfil da AWS CLI que NÃO seja o root (F01-038):
#   export AWS_PROFILE=aishac-dev
#   export PRODUCT_IMAGES_BUCKET=aishac-product-images-dev-<sufixo-unico>
#   export AWS_IAM_USER=aishac-dev            # opcional: anexa a política a este usuário IAM
#   ./infra/aws/dev/product-images-bucket.sh
set -euo pipefail

bucket="${PRODUCT_IMAGES_BUCKET:?Defina PRODUCT_IMAGES_BUCKET (nome globalmente único, ex.: aishac-product-images-dev-xyz)}"
region="${AWS_REGION:-sa-east-1}"
iam_user="${AWS_IAM_USER:-}"
policy_name="aishac-sales-service-product-images-dev"

command -v aws >/dev/null || { echo "AWS CLI v2 não encontrada no PATH." >&2; exit 1; }

account=$(aws sts get-caller-identity --query Account --output text)
caller=$(aws sts get-caller-identity --query Arn --output text)
if [[ "$caller" == *":root" ]]; then
  echo "Você está usando a conta root. Use um usuário IAM (F01-038)." >&2
  exit 1
fi
policy_arn="arn:aws:iam::${account}:policy/${policy_name}"

cat <<EOF
Plano (conta ${account}, identidade ${caller}):
  1. Bucket ${bucket} em ${region}: privado (Block Public Access), sem ACL (BucketOwnerEnforced),
     criptografia SSE-S3, ciclo de vida só para uploads multipart incompletos (1 dia), tags.
  2. Política IAM ${policy_name}: Put/Get/Delete em products/* e List no bucket.
  3. Anexar a política ao usuário: ${iam_user:-(nenhum; defina AWS_IAM_USER se quiser)}
Custo esperado: centavos (S3 Standard por GB armazenado e por requisição). Configure o AWS Budgets antes (F01-038).
EOF
read -r -p "Continuar? [s/N] " answer
[[ "$answer" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

# 1. Bucket ---------------------------------------------------------------------------------------
if head_error=$(aws s3api head-bucket --bucket "$bucket" 2>&1); then
  echo "Bucket ${bucket} já existe na sua conta: reaplicando as configurações."
elif [[ "$head_error" == *"403"* ]]; then
  echo "O nome ${bucket} já pertence a outra conta. Escolha outro PRODUCT_IMAGES_BUCKET." >&2
  exit 1
else
  aws s3api create-bucket --bucket "$bucket" --region "$region" \
    --create-bucket-configuration "LocationConstraint=${region}" >/dev/null
  echo "Bucket ${bucket} criado."
fi

aws s3api put-public-access-block --bucket "$bucket" --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

aws s3api put-bucket-ownership-controls --bucket "$bucket" \
  --ownership-controls 'Rules=[{ObjectOwnership=BucketOwnerEnforced}]'

aws s3api put-bucket-encryption --bucket "$bucket" --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"},"BucketKeyEnabled":true}]}'

# Nenhuma regra de expiração de objetos: o S3 não distingue órfão de imagem válida, e uma expiração no bucket
# apagaria as imagens dos produtos. Órfãos são uma limitação aceita da fase 01 (ADR-010, IF-026).
aws s3api put-bucket-lifecycle-configuration --bucket "$bucket" --lifecycle-configuration \
  '{"Rules":[{"ID":"abort-incomplete-multipart-uploads","Status":"Enabled","Filter":{},"AbortIncompleteMultipartUpload":{"DaysAfterInitiation":1}}]}'

aws s3api put-bucket-tagging --bucket "$bucket" \
  --tagging 'TagSet=[{Key=project,Value=aishac-shop},{Key=env,Value=dev},{Key=service,Value=sales-service}]'
echo "Bucket configurado."

# 2. Política IAM (mínimo privilégio, rule storage) ------------------------------------------------
policy_document=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ProductImageObjects",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::${bucket}/products/*"
    },
    {
      "Sid": "ListProductImages",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::${bucket}",
      "Condition": {"StringLike": {"s3:prefix": "products/*"}}
    }
  ]
}
EOF
)

if aws iam get-policy --policy-arn "$policy_arn" >/dev/null 2>&1; then
  # A IAM guarda até 5 versões: remove a mais antiga não padrão antes de criar outra.
  versions=$(aws iam list-policy-versions --policy-arn "$policy_arn" \
    --query 'Versions[?IsDefaultVersion==`false`].VersionId' --output text)
  if [[ $(wc -w <<<"$versions") -ge 4 ]]; then
    oldest=$(tr '\t' '\n' <<<"$versions" | sort -V | head -1)
    aws iam delete-policy-version --policy-arn "$policy_arn" --version-id "$oldest"
  fi
  aws iam create-policy-version --policy-arn "$policy_arn" --policy-document "$policy_document" \
    --set-as-default >/dev/null
  echo "Política ${policy_name} atualizada."
else
  aws iam create-policy --policy-name "$policy_name" --policy-document "$policy_document" \
    --description "sales-service (dev): imagens de produto no bucket ${bucket}" \
    --tags Key=project,Value=aishac-shop Key=env,Value=dev >/dev/null
  echo "Política ${policy_name} criada."
fi

# 3. Anexo ao usuário (opcional) -------------------------------------------------------------------
if [[ -n "$iam_user" ]]; then
  aws iam attach-user-policy --user-name "$iam_user" --policy-arn "$policy_arn"
  echo "Política anexada ao usuário ${iam_user}."
fi

cat <<EOF

Pronto. Para o sales-service no perfil dev:
  PRODUCT_IMAGES_BUCKET=${bucket}
  AWS_PROFILE=<perfil com a política anexada>   (cadeia padrão de credenciais do SDK; nada no config-repo)
EOF
