# Referência S3

## Magic numbers aceitos
| Tipo | Bytes iniciais |
|---|---|
| JPEG | `FF D8 FF` |
| PNG | `89 50 4E 47 0D 0A 1A 0A` |
| WebP | `52 49 46 46` (RIFF) + bytes 8–11 `57 45 42 50` (WEBP) |

## Chave do objeto
`products/{productId}/{imageId}.{ext}` — `imageId` é UUID v7 novo a cada upload ou substituição.

## Política IAM mínima (perfil dev)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": ["s3:ListBucket"], "Resource": "arn:aws:s3:::<bucket>" },
    { "Effect": "Allow", "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"], "Resource": "arn:aws:s3:::<bucket>/products/*" }
  ]
}
```

## LocalStack
- Requer `LOCALSTACK_AUTH_TOKEN` (plano Hobby) no ambiente do usuário.
- `path-style-access=true`; credenciais fictícias (`test`/`test`) só no perfil `local` e nos testes.
