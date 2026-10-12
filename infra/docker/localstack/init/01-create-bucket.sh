#!/usr/bin/env bash
# Executado pelo LocalStack quando fica pronto (ready.d). Cria o bucket privado de imagens de produto.
set -euo pipefail

bucket="${PRODUCT_IMAGES_BUCKET:-aishac-product-images-local}"

awslocal s3api create-bucket --bucket "$bucket" >/dev/null
awslocal s3api put-public-access-block --bucket "$bucket" \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

echo "Bucket '$bucket' pronto."
