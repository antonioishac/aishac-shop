#!/usr/bin/env bash
# F01-037 — Remove o bucket de imagens do perfil dev (com TODOS os objetos) e a política IAM do sales-service.
# Executado APENAS pelo usuário (premissa P3). Irreversível: as imagens apagadas não voltam.
#
# Uso (Git Bash):
#   export AWS_PROFILE=aishac-dev
#   export PRODUCT_IMAGES_BUCKET=aishac-product-images-dev-<sufixo-unico>
#   export AWS_IAM_USER=aishac-dev            # opcional: desanexa a política deste usuário
#   ./infra/aws/dev/product-images-bucket-remove.sh
set -euo pipefail

bucket="${PRODUCT_IMAGES_BUCKET:?Defina PRODUCT_IMAGES_BUCKET}"
iam_user="${AWS_IAM_USER:-}"
policy_name="aishac-sales-service-product-images-dev"

command -v aws >/dev/null || { echo "AWS CLI v2 não encontrada no PATH." >&2; exit 1; }
account=$(aws sts get-caller-identity --query Account --output text)
policy_arn="arn:aws:iam::${account}:policy/${policy_name}"

echo "Isto apaga o bucket ${bucket} com todos os objetos e a política ${policy_name} (conta ${account})."
read -r -p "Digite o nome do bucket para confirmar: " answer
[[ "$answer" == "$bucket" ]] || { echo "Cancelado."; exit 0; }

if aws s3api head-bucket --bucket "$bucket" >/dev/null 2>&1; then
  aws s3 rm "s3://${bucket}" --recursive
  aws s3api delete-bucket --bucket "$bucket"
  echo "Bucket ${bucket} removido."
else
  echo "Bucket ${bucket} não existe nesta conta."
fi

if aws iam get-policy --policy-arn "$policy_arn" >/dev/null 2>&1; then
  if [[ -n "$iam_user" ]]; then
    aws iam detach-user-policy --user-name "$iam_user" --policy-arn "$policy_arn" 2>/dev/null || true
  fi
  attached=$(aws iam list-entities-for-policy --policy-arn "$policy_arn" \
    --query 'length(PolicyUsers)+length(PolicyGroups)+length(PolicyRoles)' --output text)
  if [[ "$attached" != "0" ]]; then
    echo "A política ainda está anexada a ${attached} identidade(s). Desanexe e rode de novo." >&2
    exit 1
  fi
  for version in $(aws iam list-policy-versions --policy-arn "$policy_arn" \
      --query 'Versions[?IsDefaultVersion==`false`].VersionId' --output text); do
    aws iam delete-policy-version --policy-arn "$policy_arn" --version-id "$version"
  done
  aws iam delete-policy --policy-arn "$policy_arn"
  echo "Política ${policy_name} removida."
fi
