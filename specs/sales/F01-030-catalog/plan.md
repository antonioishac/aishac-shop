# Plano técnico — F01-030 (implementação F01-031 a F01-036 e F01-039)

## Visão geral
Módulo `catalog` do sales-service em `com.aishac.sales.catalog`, com as camadas por tipo dentro (ADR-025, ADR-026). Controllers finos; regras nos services. O estoque muda só por comandos atômicos no banco (fora do `@Version`). Imagens: arquivo no S3 fora da transação, gravação no banco com o produto travado, compensação em caso de falha.

## Arquivos
| Camada | Arquivo | Ação |
|---|---|---|
| raiz (transversal) | `entity/BaseEntity`, `dto/PageResponse`, `exception/ErrorCode`… (já existentes) | criar / reaproveitar |
| catalog/entity | `Category`, `Product`, `StockMovement`, `StockMovementType`, `ProductImage` | criar |
| catalog/repository | `CategoryRepository`, `ProductRepository` (saída/entrada atômica com `RETURNING`, trava para imagens), `StockMovementRepository`, `ProductImageRepository` | criar |
| catalog/specification | `CatalogSpecifications` (categorias e produtos) | criar |
| catalog/mapper | `CatalogMapper` (MapStruct) | criar |
| catalog/service | `CategoryService`, `ProductService`, `StockService`, `ProductImageService`, `ProductImageStorage`, `ImageType` (detecção pelo magic number), `CatalogRules`; `impl/*Impl`, `impl/S3ProductImageStorage` | criar |
| catalog/dto | requests e responses de categoria, produto, estoque e imagem | criar |
| catalog/controller | `CategoryController`, `ProductController`, `StockMovementController`, `ProductImageController` | criar |
| catalog/exception | `CatalogErrorCode` | criar |
| catalog/config | `StorageProperties` (`aishac.storage.*`), `StorageConfiguration` (`S3Client`, `S3Presigner`) | criar |
| resources/db | `V1`..`V4` em `db/migration`; `R__seed_catalog.sql` em `db/seed` (só perfil `local`) | criar |
| test | unitários dos services, `CatalogApiIT` (validação de entrada inclusa), `StockConcurrencyIT`, `S3ProductImageStorageIT` (LocalStack, só com token) | criar |

## Padrões aplicados
| Padrão | Onde | Por quê |
|---|---|---|
| Service layer (interface + impl) | `catalog/service` | Regras de negócio num só lugar |
| Specification | filtros de categoria e produto | Filtros combináveis |
| Strategy (interface de armazenamento) | `ProductImageStorage` + `S3ProductImageStorage` | S3 trocável por implementação em memória nos testes |
| Compensação | upload e substituição de imagem | Nunca deixar objeto sem registro por erro da API (RN-21) |

## Resiliência e concorrência
- **Estoque:** `UPDATE ... WHERE stock_quantity >= :qty RETURNING stock_quantity` (saída) e `UPDATE ... RETURNING` (entrada); a coluna é `updatable = false` na entidade e não incrementa a `version`.
- **Imagens:** `SELECT ... FOR UPDATE` no produto (`PESSIMISTIC_WRITE`, sem incrementar versão) só durante a gravação no banco; upload ao S3 antes, sem trava.
- **S3:** Circuit Breaker do Resilience4j (instância `s3`) na implementação; o retry e os timeouts ficam com o próprio AWS SDK (retry padrão do SDK, `apiCallTimeout`), para não multiplicar retentativas. Falha ou circuito aberto → `503 IMAGE_STORAGE_UNAVAILABLE`. URL pré-assinada é calculada localmente (não chama o S3).
- **Unicidade** (SKU, nome de categoria): checagem prévia + UNIQUE traduzida pelo nome da constraint.

## Configuração nova
| Propriedade | local | dev | prod |
|---|---|---|---|
| `aishac.storage.bucket` | `${PRODUCT_IMAGES_BUCKET:aishac-product-images-local}` | `${PRODUCT_IMAGES_BUCKET}` | `${PRODUCT_IMAGES_BUCKET}` |
| `aishac.storage.region` | `${AWS_REGION:us-east-1}` | `sa-east-1` | `sa-east-1` |
| `aishac.storage.endpoint` | `${AWS_S3_ENDPOINT:http://localhost:4566}` | (vazio = AWS) | (vazio) |
| `aishac.storage.path-style-access` | `true` | `false` | `false` |
| `aishac.storage.presigned-url-ttl` | `15m` | `15m` | `15m` |
| `aishac.storage.max-file-size` | `5MB` | `5MB` | `5MB` |
| `spring.servlet.multipart.max-file-size` | `10MB` (acima do limite da aplicação, para responder `IMAGE_TOO_LARGE`) | idem | idem |
| `spring.flyway.locations` | `classpath:db/migration,classpath:db/seed` | só `db/migration` | só `db/migration` |
| `resilience4j.circuitbreaker.instances.s3.*` | janela 20, falha 50%, aberto 10 s | idem | idem |

## Dependências novas
- `software.amazon.awssdk:s3` (BOM 2.55.15): cliente S3 e presigner.
- `io.github.resilience4j:resilience4j-spring-boot4` (2.4.0) + `spring-boot-starter-aspectj`: Circuit Breaker por anotação.
- `org.testcontainers:testcontainers-localstack` (teste).

## Riscos
- Sem `LOCALSTACK_AUTH_TOKEN`, o teste contra o S3 real (LocalStack) fica pulado; os fluxos de imagem são testados com armazenamento em memória.
- O seed roda também nos `*IT` (perfil `local`): os testes não podem supor banco vazio.
