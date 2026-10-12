# ADR-010 — Imagens de produto no S3, com upload pelo backend

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D11 · F01-035 · F01-036 · rule `storage` |

## Contexto
Produtos têm imagens que precisam ser enviadas pelo `/admin`, exibidas na loja e substituídas ou excluídas. Guardar binários no banco pesa backups e consultas; expor o bucket publicamente é um risco.

## Decisão
- Upload **via backend** (multipart: frontend → BFF em streaming → sales-service → S3), com validação por *magic number* e limite de tamanho configurável.
- Bucket **privado** (Block Public Access); leitura por **URL pré-assinada** de curta duração.
- O banco guarda a **chave** do objeto, nunca a URL.
- Substituição grava um objeto com **chave nova**.
- Exclusão física: **banco primeiro, S3 depois**. Um objeto órfão é preferível a um registro apontando para o nada.
- Acesso pelo port `ProductImageStorage` e por um adapter com AWS SDK v2 (cadeia padrão de credenciais) e Resilience4j (ADR-009).

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Binário no PostgreSQL | Transação única | Banco e backups inchados; sem CDN |
| Upload direto do navegador ao S3 (URL pré-assinada de PUT) | Não passa pelo backend | Validação do arquivo fica fora do servidor; fluxo mais complexo para o estudo |
| **Upload via backend, leitura pré-assinada** | Validação centralizada; bucket fechado | Backend trafega o arquivo (mitigado com streaming) |

## Consequências
- **Positivas:** nenhum objeto público; troca de imagem não sofre com cache de URL antiga.
- **Negativas / custos:** objetos órfãos possíveis na exclusão (limpeza pela regra de ciclo de vida, F01-037); URLs expiram e precisam ser regeneradas a cada leitura.
- **O que observar:** a URL pré-assinada gerada **dentro do Docker** aponta para `localstack:4566`, que não resolve no navegador. Prever um *endpoint* público distinto no perfil `local` ao implementar o F01-036.
