# ADR-010 — Imagens de produto no S3, com upload pelo backend

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D11 · F01-030 · F01-035 · F01-036 · rule `storage` |

## Contexto
Produtos têm imagens que precisam ser enviadas pelo `/admin`, exibidas na loja e substituídas ou excluídas. Guardar binários no banco pesa backups e consultas; expor o bucket publicamente é um risco.

## Decisão
- Upload **via backend** (multipart: frontend → BFF em streaming → sales-service → S3), com validação por *magic number* e limite de tamanho configurável.
- Bucket **privado** (Block Public Access); leitura por **URL pré-assinada** de curta duração.
- O banco guarda a **chave** do objeto, nunca a URL.
- Substituição grava um objeto com **chave nova**.
- Exclusão física: **banco primeiro, S3 depois**. Um objeto órfão é preferível a um registro apontando para o nada.
- Acesso pela interface `ProductImageStorage` e por uma implementação com AWS SDK v2 (cadeia padrão de credenciais) e Resilience4j (ADR-009).

### Como ficou no código (F01-030, 2026-10-13)
Com a troca para camadas por tipo (ADR-025) e módulos (ADR-026), o "port/adapter" virou interface + implementação:
- `catalog.service.ProductImageStorage` (interface: `put`, `delete`, `presign`) e `catalog.service.impl.S3ProductImageStorage` (package-private). Só o `ProductImageServiceImpl` a usa; o ArchUnit impede que outros módulos a acessem.
- **Chave do objeto:** `products/{productId}/{uuid}.{ext}`, com um UUID aleatório novo a cada arquivo gravado (inserção ou substituição) e a extensão do tipo detectado pelo *magic number* (JPEG, PNG, WebP). Não usa o ID da imagem: a substituição mantém o registro e troca só a chave.
- **Inserir:** valida tamanho e tipo → `PutObject` → transação curta com o produto travado (`SELECT ... FOR UPDATE`, `findForImageChange`) para conferir o limite de imagens, escolher a posição e a principal → se o banco falhar, apaga o objeto recém-gravado (compensação). O S3 nunca fica dentro da transação.
- **Substituir:** `PutObject` com chave nova → transação curta que troca a chave (produto travado) → remove o objeto antigo; se o banco falhar, apaga o novo.
- **Excluir:** transação que remove o registro (e promove outra imagem a principal, se for o caso) → `DeleteObject`. Falha no S3 só gera log de aviso e incrementa a métrica `aishac.storage.orphaned.objects` (objeto órfão, sem limpeza automática na fase 01).
- **URL pré-assinada:** calculada localmente pelo `S3Presigner`, a cada leitura, com validade `aishac.storage.presigned-url-ttl` (padrão 15 min).
- **Endpoint público para o navegador:** `aishac.storage.endpoint` é o endereço que o serviço usa (`http://localstack:4566` dentro do Docker); `aishac.storage.public-endpoint` é o usado só pelo `S3Presigner` (`http://localhost:4566` no perfil `local`). Vazio, vale o `endpoint`; ambos vazios, AWS real (`dev`/`prod`).
- Integridade no banco (`product_images`): chave única, posição única por produto e uma única principal por produto, as duas últimas **adiáveis** (checadas no COMMIT), para permitir trocas de posição e de principal na mesma transação.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Binário no PostgreSQL | Transação única | Banco e backups inchados; sem CDN |
| Upload direto do navegador ao S3 (URL pré-assinada de PUT) | Não passa pelo backend | Validação do arquivo fica fora do servidor; fluxo mais complexo para o estudo |
| **Upload via backend, leitura pré-assinada** | Validação centralizada; bucket fechado | Backend trafega o arquivo (mitigado com streaming) |

## Consequências
- **Positivas:** nenhum objeto público; troca de imagem não sofre com cache de URL antiga.
- **Negativas / custos:** objetos órfãos possíveis na exclusão, **sem limpeza automática** na fase 01: uma regra de ciclo de vida não distingue órfão de imagem válida (expirar objetos apagaria as imagens dos produtos), e marcar o órfão exigiria o mesmo S3 que acabou de falhar. Decisão de 2026-10-13: aceitar como limitação observável (log + métrica) e deixar a reconciliação banco × bucket para a IF-026. O ciclo de vida do bucket (F01-037) só aborta uploads multipart incompletos; URLs expiram e precisam ser regeneradas a cada leitura.
- **O que observar:** a URL pré-assinada gerada **dentro do Docker** apontaria para `localstack:4566`, que não resolve no navegador. Resolvido no F01-030 com o `aishac.storage.public-endpoint` (variável `AWS_S3_PUBLIC_ENDPOINT` no `compose.apps.yml`). Continua valendo observar a janela entre o `PutObject` e o COMMIT (objeto sem registro se o processo cair antes da compensação) e o volume de órfãos.
