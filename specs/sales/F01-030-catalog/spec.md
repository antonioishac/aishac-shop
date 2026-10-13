# F01-030 — Catálogo

| Campo | Valor |
|---|---|
| Item do backlog | F01-030 (spec do marco 01.3; implementada por F01-031 a F01-036 e F01-039) |
| Contexto / serviço | sales-service, módulo `catalog` |
| Fase | 01-base-sincrona |
| Status | Rascunho |

## 1. Objetivo
Manter o catálogo do Aishac Shop: categorias, produtos com preço e estoque, e imagens de produto guardadas no S3. O catálogo alimenta a vitrine da loja (F01-062), o painel `/admin` (F01-066) e o módulo de pedidos do próprio sales-service, que lê preço e baixa estoque na criação do pedido (F01-042).

## 2. Regras de negócio

### Categorias
| ID | Regra |
|---|---|
| RN-01 | A categoria tem **nome** (2 a 80 caracteres) e **descrição** opcional (até 500). Há um só nível: não existe subcategoria (IF-006). |
| RN-02 | O nome da categoria é **único sem diferenciar maiúsculas e minúsculas**, entre ativas e inativas. |
| RN-03 | A categoria nasce **ativa** e pode ser desativada e reativada. **Não existe exclusão** de categoria: produtos e pedidos antigos continuam referenciando-a. |
| RN-04 | Desativar uma categoria **não altera** os produtos dela, mas eles deixam de ser **vendáveis** (RN-12). |

### Produtos
| ID | Regra |
|---|---|
| RN-05 | O produto tem **SKU**, **nome** (3 a 150 caracteres), **descrição** opcional (até 2.000), **preço**, **categoria** e **estoque**. |
| RN-06 | O **SKU** é o código do produto: 3 a 40 caracteres, só letras, dígitos e hífen, guardado em maiúsculas; **único** entre todos os produtos e **imutável** depois do cadastro. |
| RN-07 | O **preço** é maior que zero, tem no máximo 2 casas decimais e trafega como string decimal (`"129.90"`, ADR-014). Moeda: BRL. |
| RN-08 | O produto só pode ser cadastrado ou movido para uma categoria **ativa**. |
| RN-09 | O produto nasce **ativo**, com o **estoque inicial** informado (zero ou mais). Pode ser desativado e reativado; **não existe exclusão** (pedidos guardam o produto). |
| RN-10 | A atualização do produto (nome, descrição, preço, categoria) usa **lock otimista**: a requisição informa a `version` lida. O **estoque não muda por atualização**, só por ajuste (RN-13) e, depois, pelos pedidos. |
| RN-11 | A listagem é paginada e filtra por categoria, parte do nome (sem diferenciar maiúsculas), faixa de preço, situação (ativo/inativo) e vendável; ordena por nome, preço ou data de cadastro. |
| RN-12 | Um produto é **vendável** quando ele e a categoria dele estão ativos. É o que a loja exibe e o que o pedido aceita (F01-042). O estoque zerado não tira o produto da vitrine; aparece como indisponível. |

### Estoque
| ID | Regra |
|---|---|
| RN-13 | O admin ajusta o estoque com uma **movimentação**: tipo `ENTRY` (entrada) ou `EXIT` (saída), **quantidade** (1 a 100.000) e **motivo** (3 a 255 caracteres). |
| RN-14 | A saída é **atômica**: só acontece se houver estoque suficiente no momento da gravação; caso contrário, nada muda e a resposta é "estoque insuficiente". O estoque nunca fica negativo (garantido também pelo banco). |
| RN-15 | Toda movimentação fica registrada (tipo, quantidade, motivo, saldo resultante, quando), e o histórico é consultável por produto. Os pedidos (F01-042, F01-047) registrarão suas próprias movimentações no mesmo histórico. |

### Imagens
| ID | Regra |
|---|---|
| RN-16 | O produto tem **até 8 imagens**. Formatos aceitos: **JPEG, PNG e WebP**, identificados pelo conteúdo do arquivo (*magic number*), nunca pelo nome nem pelo `Content-Type` informado. Tamanho máximo configurável (padrão **5 MB**). |
| RN-17 | Cada imagem tem **texto alternativo** (acessibilidade; até 150 caracteres, obrigatório), **posição** de exibição e a marcação de **principal**. A **primeira** imagem enviada vira principal; há no máximo **uma** principal por produto. |
| RN-18 | O arquivo fica no **S3, em bucket privado**; o banco guarda só a **chave** do objeto. A leitura devolve uma **URL pré-assinada** que expira (padrão 15 minutos, configurável) (ADR-010). |
| RN-19 | **Substituir** o arquivo grava um objeto com **chave nova**, atualiza o banco e só então remove o objeto antigo. A imagem mantém id, texto alternativo, posição e marcação de principal. |
| RN-20 | **Excluir** remove o registro do banco e depois o objeto do S3. Se a exclusão no S3 falhar, a imagem continua excluída para a API e o objeto órfão é removido depois pela regra de ciclo de vida do bucket. Ao excluir a principal, a próxima pela posição vira principal. |
| RN-21 | Se o banco falhar depois do upload, o objeto recém-enviado é removido (compensação): nunca fica arquivo sem registro por erro da API. |
| RN-22 | Se o S3 estiver indisponível, upload, substituição e leitura de URLs respondem "armazenamento indisponível" (503), sem alterar o banco (Resilience4j, ADR-009). |

## 3. Critérios de aceite

### CA-01 — Cadastrar categoria
- **Dado** que não existe categoria "Calçados"
- **Quando** cadastro a categoria "Calçados"
- **Então** recebo `201` com `Location`, a categoria ativa e `version` = `0`

### CA-02 — Nome de categoria duplicado
- **Dado** uma categoria "Calçados", ativa ou inativa
- **Quando** tento cadastrar ou renomear outra para "calçados"
- **Então** recebo `409` com `code` = `CATEGORY_NAME_ALREADY_EXISTS`

### CA-03 — Desativar e reativar categoria
- **Dado** uma categoria ativa com produtos ativos
- **Quando** desativo a categoria
- **Então** ela fica inativa, os produtos continuam ativos, mas deixam de aparecer no filtro `sellable=true`; ao reativar, voltam a aparecer

### CA-04 — Listar categorias
- **Dado** categorias ativas e inativas
- **Quando** listo com `active=true`
- **Então** recebo só as ativas, paginadas e ordenadas por nome

### CA-05 — Cadastrar produto
- **Dado** uma categoria ativa
- **Quando** cadastro o produto SKU `tenis-run-42`, "Tênis Run", preço `"299.90"`, estoque inicial `10`
- **Então** recebo `201` com `sku` = `TENIS-RUN-42`, `price` = `"299.90"`, `stockQuantity` = `10`, ativo e `version` = `0`; o estoque inicial aparece no histórico como uma entrada

### CA-06 — Validações do produto
- **Dado** um preço `"0.00"`, `"-1"`, `"10.999"` ou `"abc"`, um SKU com espaço ou um nome com 2 caracteres
- **Quando** tento cadastrar
- **Então** recebo `400` com `code` = `VALIDATION_ERROR` e `errors[]` listando cada campo

### CA-07 — SKU duplicado
- **Dado** um produto com SKU `TENIS-RUN-42`
- **Quando** tento cadastrar outro com `tenis-run-42`
- **Então** recebo `409` com `code` = `SKU_ALREADY_EXISTS`

### CA-08 — Categoria inexistente ou inativa
- **Dado** uma categoria inativa e um id de categoria inexistente
- **Quando** tento cadastrar ou mover um produto para elas
- **Então** recebo `422` com `CATEGORY_INACTIVE` e `404` com `CATEGORY_NOT_FOUND`, respectivamente

### CA-09 — Atualizar produto com lock otimista
- **Dado** um produto com `version` = `0` e estoque `10`
- **Quando** altero nome e preço informando `version` = `0`, e depois repito a alteração com `version` = `0`
- **Então** a primeira recebe `200` com `version` = `1` e estoque ainda `10`; a segunda recebe `409` com `CONCURRENT_MODIFICATION`, sem alterar nada

### CA-10 — Listar produtos com filtros
- **Dado** produtos de categorias e preços diferentes, alguns inativos
- **Quando** listo com `categoryId`, `name=tenis`, `minPrice=100.00`, `maxPrice=300.00`, `sellable=true` e `sort=price,asc`
- **Então** recebo só os produtos que atendem a todos os filtros, paginados e ordenados por preço

### CA-11 — Desativar e reativar produto
- **Dado** um produto ativo
- **Quando** desativo e depois reativo
- **Então** ele continua consultável por id, sai e volta do filtro `sellable=true`

### CA-12 — Entrada de estoque
- **Dado** um produto com estoque `10`
- **Quando** registro uma entrada de `5` com motivo "Reposição do fornecedor"
- **Então** recebo `201` com a movimentação e saldo `15`; o produto passa a ter `stockQuantity` = `15`

### CA-13 — Saída maior que o estoque
- **Dado** um produto com estoque `3`
- **Quando** registro uma saída de `5`
- **Então** recebo `422` com `code` = `INSUFFICIENT_STOCK`, e o estoque continua `3`

### CA-14 — Saídas simultâneas
- **Dado** um produto com estoque `1`
- **Quando** 10 saídas de `1` chegam ao mesmo tempo
- **Então** exatamente uma é aceita, as demais recebem `INSUFFICIENT_STOCK`, e o estoque final é `0`

### CA-15 — Histórico de estoque
- **Dado** um produto com entradas e saídas registradas
- **Quando** consulto o histórico
- **Então** recebo as movimentações da mais recente para a mais antiga, paginadas, com tipo, quantidade, motivo, saldo resultante e data

### CA-16 — Enviar imagem
- **Dado** um produto sem imagens
- **Quando** envio um JPEG válido de 200 KB com texto alternativo "Tênis Run azul, vista lateral"
- **Então** recebo `201` com a imagem marcada como principal, posição `1` e uma `url` pré-assinada que baixa o arquivo enviado

### CA-17 — Tipo de arquivo inválido
- **Dado** um arquivo de texto renomeado para `foto.jpg` e enviado como `image/jpeg`
- **Quando** tento enviá-lo
- **Então** recebo `400` com `code` = `INVALID_IMAGE_TYPE`, e nada é gravado no S3 nem no banco

### CA-18 — Arquivo grande demais e limite de imagens
- **Dado** um arquivo de 6 MB, e outro produto que já tem 8 imagens
- **Quando** tento enviar
- **Então** recebo `400` com `IMAGE_TOO_LARGE` e `422` com `IMAGE_LIMIT_EXCEEDED`, respectivamente

### CA-19 — Substituir arquivo
- **Dado** uma imagem principal com a chave `products/{productId}/{imageId-antigo}.jpg`
- **Quando** envio um novo arquivo PNG para ela
- **Então** a imagem mantém id, texto alternativo, posição e principal; a chave passa a ser nova; o objeto antigo deixa de existir no bucket

### CA-20 — Atualizar metadados
- **Dado** um produto com as imagens A (principal, posição 1) e B (posição 2)
- **Quando** marco B como principal, troco as posições e altero o texto alternativo de A
- **Então** B passa a principal e A deixa de ser, as posições ficam trocadas, e o arquivo de nenhuma delas muda

### CA-21 — Excluir imagem
- **Dado** um produto com as imagens A (principal, posição 1) e B (posição 2)
- **Quando** excluo A
- **Então** recebo `204`, B vira principal, e o objeto de A não existe mais no bucket

### CA-22 — Falha do banco depois do upload
- **Dado** que o banco falha ao gravar a imagem, depois de o arquivo já estar no S3
- **Quando** o upload termina com erro
- **Então** o objeto recém-enviado é removido do bucket, e a resposta é de erro

### CA-23 — S3 indisponível
- **Dado** que o S3 não responde
- **Quando** envio ou substituo uma imagem
- **Então** recebo `503` com `code` = `IMAGE_STORAGE_UNAVAILABLE`, e o banco não muda

## 4. Endpoints

Base: `/api/v1`. Todos aceitam e devolvem `X-Correlation-Id`.

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `POST` | `/categories` | Cadastrar categoria | 201 + `Location` | 400, 409 |
| `GET` | `/categories` | Listar paginado (filtros `name`, `active`) | 200 | 400 |
| `GET` | `/categories/{categoryId}` | Buscar por id | 200 | 404 |
| `PUT` | `/categories/{categoryId}` | Atualizar nome e descrição (com `version`) | 200 | 400, 404, 409 |
| `POST` | `/categories/{categoryId}/deactivate` | Desativar | 200 | 404 |
| `POST` | `/categories/{categoryId}/activate` | Reativar | 200 | 404 |
| `POST` | `/products` | Cadastrar produto | 201 + `Location` | 400, 404, 409, 422 |
| `GET` | `/products` | Listar paginado (filtros `categoryId`, `name`, `minPrice`, `maxPrice`, `active`, `sellable`) | 200 | 400 |
| `GET` | `/products/{productId}` | Buscar por id (com a imagem principal) | 200 | 404 |
| `PUT` | `/products/{productId}` | Atualizar nome, descrição, preço e categoria (com `version`) | 200 | 400, 404, 409, 422 |
| `POST` | `/products/{productId}/deactivate` | Desativar | 200 | 404 |
| `POST` | `/products/{productId}/activate` | Reativar | 200 | 404 |
| `POST` | `/products/{productId}/stock-movements` | Registrar entrada ou saída | 201 | 400, 404, 422 |
| `GET` | `/products/{productId}/stock-movements` | Histórico de estoque paginado | 200 | 404 |
| `POST` | `/products/{productId}/images` | Enviar imagem (`multipart/form-data`: `file`, `altText`) | 201 + `Location` | 400, 404, 422, 503 |
| `GET` | `/products/{productId}/images` | Listar imagens com URL pré-assinada | 200 | 404, 503 |
| `GET` | `/products/{productId}/images/{imageId}` | Buscar imagem com URL pré-assinada | 200 | 404, 503 |
| `PUT` | `/products/{productId}/images/{imageId}/file` | Substituir o arquivo (`multipart/form-data`: `file`) | 200 | 400, 404, 503 |
| `PATCH` | `/products/{productId}/images/{imageId}` | Atualizar texto alternativo, posição e/ou principal | 200 | 400, 404 |
| `DELETE` | `/products/{productId}/images/{imageId}` | Excluir | 204 | 404 |

Contrato completo: [contract.yaml](contract.yaml)

## 5. Erros

| Código (`code`) | HTTP | Quando |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Campo obrigatório ausente ou formato inválido (lista em `errors[]`) |
| `INVALID_IMAGE_TYPE` | 400 | Arquivo que não é JPEG, PNG nem WebP pelo conteúdo |
| `IMAGE_TOO_LARGE` | 400 | Arquivo acima do limite configurado |
| `CATEGORY_NOT_FOUND` | 404 | Categoria inexistente |
| `PRODUCT_NOT_FOUND` | 404 | Produto inexistente |
| `IMAGE_NOT_FOUND` | 404 | Imagem inexistente ou de outro produto |
| `CATEGORY_NAME_ALREADY_EXISTS` | 409 | Nome de categoria já usado (sem diferenciar maiúsculas) |
| `SKU_ALREADY_EXISTS` | 409 | SKU já usado |
| `CONCURRENT_MODIFICATION` | 409 | `version` desatualizada |
| `CATEGORY_INACTIVE` | 422 | Produto cadastrado ou movido para categoria inativa |
| `INSUFFICIENT_STOCK` | 422 | Saída maior que o estoque disponível |
| `IMAGE_LIMIT_EXCEEDED` | 422 | 9ª imagem do produto |
| `IMAGE_STORAGE_UNAVAILABLE` | 503 | S3 indisponível (circuito aberto, timeout ou erro) |

Todos em ProblemDetail com `code`, `correlationId` e `timestamp` (ADR-008). Valem também os códigos transversais do serviço (`MALFORMED_REQUEST`, `ROUTE_NOT_FOUND`, `METHOD_NOT_ALLOWED`, `DATA_INTEGRITY_VIOLATION`, `INTERNAL_ERROR`).

## 6. Modelo de dados
> *Pendente — preenchido pela skill `modelar-dados` e aprovado no Portão 2.*

## 7. Dependências externas
- **Amazon S3** (LocalStack no perfil `local` e nos testes; AWS no `dev`): **obrigatório** para enviar, substituir e ler imagens; indisponível → `503 IMAGE_STORAGE_UNAVAILABLE`. Listagem e consulta de produtos **não dependem** do S3: com o S3 fora do ar, o produto vem com `primaryImage` = `null` (pergunta 6).
- Configuração nova por perfil: bucket, região, endpoint (LocalStack), validade da URL pré-assinada, tamanho máximo de imagem (skill `configurar-ambiente`).
- Infra do `dev`: script AWS CLI do bucket privado com ciclo de vida de 30 dias e política IAM (F01-037, executado por você).
- Nenhuma chamada a outros serviços. O módulo `order` (F01-040) vai ler preço, vendabilidade e baixar estoque pelo próprio sales-service, em processo.

## 8. Fora de escopo
- Subcategorias (IF-006), variações de produto (tamanho, cor), promoções e preço por tabela.
- Reserva de estoque antes do checkout (ADR-015).
- Redimensionamento e miniaturas de imagem (IF-007) e CDN (IF-012).
- Busca textual ignorando acentos e busca por relevância.
- Exclusão física de categoria ou produto.

## 9. Perguntas em aberto
1. **SKU:** o backlog não cita; proponho incluir (código único, imutável, maiúsculo). Mantém?
2. **Histórico de estoque (RN-15):** proponho registrar toda movimentação, inclusive o estoque inicial e, depois, as dos pedidos. Atende, ou basta o saldo?
3. **Limite de imagens:** 8 por produto e 5 MB por arquivo estão bons?
4. **Texto alternativo obrigatório (RN-17):** proponho obrigatório, por acessibilidade. Concorda?
5. **Desativar categoria (RN-04):** mantenho os produtos ativos, só não vendáveis. Prefere bloquear a desativação enquanto houver produtos ativos?
6. **S3 fora do ar na vitrine:** proponho que a listagem e a consulta de produtos **continuem funcionando** com `primaryImage` = `null` (degradação parcial), em vez de falhar a tela. Concorda?
