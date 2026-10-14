# Tarefas — F01-030

## Mapeamento critério → teste
| Critério | Teste(s) | Nível | Status |
|---|---|---|---|
| CA-01 | `CategoryServiceImplTest.shouldCreateActiveCategory`, `CatalogApiIT.shouldCreateActiveCategory` | unitário, integração | ✅ |
| CA-02 | `CategoryServiceImplTest.shouldRejectDuplicatedNameAndTranslateUniqueRace`, `CatalogApiIT.shouldRejectDuplicatedCategoryNameIgnoringCaseEvenWhenInactive` | unitário, integração | ✅ |
| CA-03 | `CategoryServiceImplTest.shouldDeactivateAndActivate`, `CatalogApiIT.shouldHideProductsOfInactiveCategoryFromSellableFilter` | unitário, integração | ✅ |
| CA-04 | `CatalogApiIT.shouldListOnlyActiveCategoriesSortedByName` | integração | ✅ |
| CA-05 | `ProductServiceImplTest.shouldCreateProductWithUppercaseSkuAndInitialEntry`, `CatalogApiIT.shouldCreateProductWithUppercaseSkuAndInitialStockEntry` | unitário, integração | ✅ |
| CA-06 | `CatalogApiIT.shouldRejectInvalidProductFields` | integração | ✅ |
| CA-07 | `ProductServiceImplTest.shouldRejectExistingSkuAndTranslateUniqueRace`, `CatalogApiIT.shouldRejectDuplicatedSkuIgnoringCase` | unitário, integração | ✅ |
| CA-08 | `ProductServiceImplTest.shouldRejectInactiveOrUnknownCategory`, `CatalogApiIT.shouldRejectInactiveOrUnknownCategoryOnCreateAndMove` | unitário, integração | ✅ |
| CA-09 | `ProductServiceImplTest.shouldUpdateOnlyWithCurrentVersionAndKeepStock`, `CatalogApiIT.shouldUpdateProductWithOptimisticLockKeepingStock` | unitário, integração | ✅ |
| CA-10 | `CatalogApiIT.shouldCombineProductFiltersAndSortByPrice` | integração | ✅ |
| CA-11 | `ProductServiceImplTest.shouldDeactivateAndActivate`, `CatalogApiIT.shouldDeactivateAndReactivateProduct` | unitário, integração | ✅ |
| CA-12 | `StockServiceImplTest.shouldRecordEntryWithResultingStock`, `CatalogApiIT.shouldRegisterStockEntry` | unitário, integração | ✅ |
| CA-13 | `StockServiceImplTest.shouldRejectExitWhenAtomicUpdateFindsNoStock`, `CatalogApiIT.shouldRejectExitGreaterThanStock` | unitário, integração | ✅ |
| CA-14 | `StockConcurrencyIT.shouldAcceptExactlyOneExitWhenLastItemIsDisputed` | concorrência | ✅ |
| CA-15 | `CatalogApiIT.shouldListStockHistoryFromNewestToOldest` | integração | ✅ |
| CA-16 | `ProductImageServiceImplTest.shouldStoreFileAndMakeFirstImagePrimaryAtFirstFreePosition`, `CatalogApiIT.shouldUploadFirstImageAsPrimary`, `S3ProductImageStorageIT.shouldStoreAndServeObjectThroughPresignedUrl` | unitário, integração, LocalStack | ✅ (LocalStack ⏸️ sem token) |
| CA-17 | `ImageTypeTest`, `ProductImageServiceImplTest.shouldValidateFileBeforeUploading`, `CatalogApiIT.shouldRejectFileThatIsNotAnImage` | unitário, integração | ✅ |
| CA-18 | `ProductImageServiceImplTest.shouldValidateFileBeforeUploading`, `ProductImageServiceImplTest.shouldRejectNinthImageAndUnknownProductBeforeUploading`, `CatalogApiIT.shouldRejectLargeFileAndNinthImage`, `CatalogApiIT.shouldAnswerFileTooLargeAboveMultipartLimit`, `ImageConcurrencyIT.shouldAcceptOnlyTheLastFreeSlotUnderConcurrentUploads` | unitário, integração, concorrência | ✅ |
| CA-19 | `ProductImageServiceImplTest.shouldReplaceFileKeepingMetadataAndRemovingOldObject`, `CatalogApiIT.shouldReplaceFileWithNewKeyAndRemoveOldObject`, `S3ProductImageStorageIT.shouldDeleteObject` | unitário, integração, LocalStack | ✅ (LocalStack ⏸️ sem token) |
| CA-20 | `ProductImageServiceImplTest.shouldSwapPositionAndMovePrimary`, `CatalogApiIT.shouldSwapPositionsAndPrimaryWithoutTouchingFiles`, `ImageConcurrencyIT.shouldKeepSinglePrimaryUnderConcurrentChanges` | unitário, integração, concorrência | ✅ |
| CA-21 | `ProductImageServiceImplTest.shouldPromoteNextImageAndKeepDeletionWhenStorageFails`, `CatalogApiIT.shouldDeletePrimaryImageAndPromoteNext` | unitário, integração | ✅ |
| CA-22 | `ProductImageServiceImplTest.shouldRemoveUploadedObjectWhenDatabaseFails`, `ImageCompensationIT.shouldRemoveUploadedObjectWhenCommitFails` (falha no COMMIT real, constraint adiável) | unitário, integração | ✅ |
| CA-23 | `S3CircuitBreakerIT` (adapter real pelo proxy do Spring, S3 inalcançável: fallback → `IMAGE_STORAGE_UNAVAILABLE` e circuito aberto sem chamar o S3), `ProductImageServiceImplTest.shouldNotTouchDatabaseWhenStorageIsDown`, `CatalogApiIT.shouldAnswerServiceUnavailableWhenStorageIsDownWithoutChangingDatabase`, `CatalogApiIT.shouldDegradeShowcaseWhenPresignedUrlCannotBeGenerated` | unitário, integração | ✅ |

Também cobrem a feature: `ArchitectureTest` (camadas dentro dos módulos e fronteira `catalog` × `order`/`payment`, ADR-026), `CatalogRulesTest`, `ServiceSkeletonIT` (OpenAPI comparado ao `contract.yaml`: operações, status, cabeçalho de correlação, `problem+json`, `required`, enums, `$ref` e anuláveis) e os testes de validação de entrada do `CatalogApiIT` (filtro de preço, `sort` inválido, PATCH vazio, arquivo e texto alternativo ausentes).

**Pendente do usuário:** `S3ProductImageStorageIT` (adapter real contra o LocalStack) só roda com `LOCALSTACK_AUTH_TOKEN` no ambiente (F01-018). Sem o token, os fluxos de imagem são cobertos pelo armazenamento em memória.

## Passos
- [x] Spec aprovada (Portão 1)
- [x] Modelo de dados aprovado (Portão 2, DDL validado no PostgreSQL 18.4)
- [x] Migrations `V1`–`V4` e seed `R__seed_catalog` (perfil local)
- [x] Entidades, repositories e specifications
- [x] Services (regras), mapper e armazenamento (S3 + Circuit Breaker; em memória nos testes)
- [x] DTOs, controllers e OpenAPI
- [x] `./mvnw verify` verde (S3ProductImageStorageIT pulado sem token)
- [x] Coleção `.http` atualizada
- [x] Revisão `code-reviewer`: APROVADO (2026-10-13, 2ª rodada; na 1ª, B1, B2, I2, I3, I4 e as sugestões 2, 4, 5, 6 e 8 foram corrigidos)
- [x] Diagrama/ADR atualizados (ADR-008, ADR-009 com emenda do S3, ADR-010, ADR-026; páginas Componentes — sales-service, Sequência — Imagens, ER — aishac_sales e Deployment — Local)
- [x] F01-031 a F01-036 e F01-039 marcados ✅ no backlog; F01-030 fica 🔄 até a ratificação dos ajustes da spec

## Pendências registradas (fora do escopo desta entrega)
| # | Pendência | Quando |
|---|---|---|
| 1 | Tempo limite na trava de imagens (`findForImageChange`): hoje espera sem limite; no PostgreSQL exige `SET LOCAL lock_timeout`, a validar com o Hibernate 7 | Antes do F01-042 (pedido trava produtos) |
| 2 | Ordenação de produtos aceita qualquer propriedade da entidade; RN-11 prevê só `name`, `price` e `createdAt` (lista permitida no service) | Próxima mudança no catálogo |
| 3 | Ciclo de vida do bucket deve expirar **só órfãos** (tag `orphan=true` no `deleteQuietly` ou prefixo próprio), nunca imagens válidas | F01-037 |
| 4 | Teto para `stock_quantity` (hoje uma entrada muito grande estoura o `INTEGER` → 409) | Próxima mudança no catálogo |
| 5 | `UpdateProductImageRequest.isRequest()` nomeia o erro de corpo vazio pelo nome do método; alternativa: mapear no handler | Opcional |
