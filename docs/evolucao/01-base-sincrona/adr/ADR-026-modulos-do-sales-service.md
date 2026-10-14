# ADR-026 — Módulos do sales-service como pacotes com camadas internas

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-13 |
| Fase | 01-base-sincrona |
| Origem | Ponto em aberto do ADR-025 · spec F01-030 · ADR-005 · IF-010 |

## Contexto
O sales-service concentra três contextos: catálogo, pedido e pagamento (ADR-005). O catálogo pode virar serviço próprio no futuro (IF-010), então a fronteira entre eles precisa continuar visível e verificável. O ADR-025 adotou camadas por tipo (`controller`, `service`, `entity`...) e deixou para a primeira feature do sales-service a decisão de como os módulos se encaixam nessas camadas.

## Decisão
**Cada módulo é um pacote com as camadas por tipo dentro dele.** Só o que é transversal fica na raiz:

```text
com.aishac.sales/
├── config/        # transversal: Clock, auditoria, OpenAPI, correlação, logs
├── exception/     # transversal: ErrorCode, CommonErrorCode, GlobalExceptionHandler
├── catalog/       # config · controller · dto · entity · exception · mapper · repository · service(+impl) · specification
├── order/         # mesma estrutura (F01-040)
└── payment/       # mesma estrutura (F01-046)
```

- Dentro de cada módulo valem as regras do ADR-025 (controller fino, regras no service).
- Entre módulos: `catalog` não depende de `order` nem de `payment`. `order` usa o catálogo **somente pelas interfaces de service do catálogo** (nunca por repository, entity ou mapper dele) e sem FK entre tabelas de módulos diferentes.
- O catálogo de erros de cada módulo fica no `exception/` do módulo (ex.: `catalog.exception.CatalogErrorCode`), implementando o `ErrorCode` transversal.
- Configuração específica de um módulo fica no `config/` do módulo (ex.: `catalog.config.StorageConfiguration`, `StorageProperties`, `CatalogOpenApiConfiguration`); a raiz `config/` guarda só o que é de todo o serviço.
- Tipos de apoio comuns aos módulos ficam na raiz: `entity.BaseEntity` (UUID v7 e auditoria) e `dto.PageResponse` / `dto.ProblemResponse`.
- Verificado no `ArchitectureTest` do sales-service.

### Como o ArchUnit verifica (F01-030, 2026-10-13)
- `catalogShouldNotDependOnOtherModules`: nada em `catalog..` depende de `order..` ou `payment..`.
- `otherModulesShouldUseCatalogOnlyThroughItsServiceInterfaces`: classes de `order..` e `payment..` só podem depender, do catálogo, de **interfaces de `catalog.service` cujo nome termina em `Service`** (`CategoryService`, `ProductService`, `StockService`, `ProductImageService`), dos **DTOs** (`catalog.dto..`) e das **exceções / códigos de erro** (`catalog.exception..`). Ficam internos ao módulo: `service.impl`, helpers de regra (`CatalogRules`), `ProductImageStorage`, `ImageType`, `entity`, `repository`, `mapper`, `specification` e `config`.
- `packagesShouldBeFreeOfCycles`: sem ciclos entre os pacotes da raiz e entre as camadas do catálogo.
- As regras de camada (ADR-025) usam padrões `..controller..`, `..service..` etc., então valem igualmente dentro de cada módulo.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| **Módulo como pacote, camadas dentro** | Fronteira explícita e testável; extrair o catálogo é mover um pacote | Dois níveis de pacote |
| Camadas na raiz, subpacote por módulo (`controller/catalog`...) | Igual ao `cadastro-base-service` | Cada módulo espalhado em oito pacotes; fronteira difícil de enxergar e de testar |

## Consequências
- **Positivas:** a extração do catálogo (IF-010) fica mecânica; o ArchUnit pega dependências proibidas entre módulos.
- **Negativas / custos:** caminhos de pacote mais longos; o customer-service (um só contexto) segue com as camadas na raiz.
- **O que observar:** chamadas de `order` a services do catálogo que exponham entidades (devem trafegar DTOs ou tipos simples).
