# Tarefas — F01-020

## Mapeamento critério → teste
| Critério | Teste(s) | Nível | Status |
|---|---|---|---|
| CA-01 | `CustomerServiceImplTest.shouldCreateActiveCustomerWithNormalizedCpfAndEmail`, `CustomerApiIT.shouldCreateCustomerWithNormalizedData` | unitário, integração | ✅ |
| CA-02 | `CpfValidatorTest.shouldRejectInvalidCpf`, `CustomerServiceImplTest.shouldRejectInvalidCpfBeforeTouchingTheDatabase`, `CustomerApiIT.shouldRejectInvalidCpf` | unitário, integração | ✅ |
| CA-03 | `CustomerServiceImplTest.shouldRejectDuplicatedCpfOrEmail`, `CustomerServiceImplTest.shouldTranslateUniqueViolationRaisedByConcurrentRegistration`, `CustomerApiIT.shouldRejectDuplicatedCpfEvenFromInactiveCustomer`, `CustomerConcurrencyIT.shouldKeepCpfUniqueUnderConcurrentRegistrations` | unitário, integração, concorrência | ✅ |
| CA-04 | `CustomerServiceImplTest.shouldRejectDuplicatedCpfOrEmail`, `CustomerApiIT.shouldRejectDuplicatedEmailIgnoringCaseOnCreateAndUpdate`, `CustomerConcurrencyIT.shouldKeepEmailUniqueUnderConcurrentRegistrations` | unitário, integração, concorrência | ✅ |
| CA-05 | `CustomerControllerTest.shouldListEveryInvalidFieldOnCreate`, `CustomerControllerTest.shouldRequireVersionOnUpdate` | fatia | ✅ |
| CA-06 | `CustomerServiceImplTest.shouldAnswerNotFoundForUnknownCustomer`, `CustomerApiIT.shouldFindCustomerByIdOrAnswerNotFound` | unitário, integração | ✅ |
| CA-07 | `CustomerApiIT.shouldFilterSortAndPaginate` | integração | ✅ |
| CA-08 | `CustomerApiIT.shouldFindCustomerByCpfOrEmailForCheckout` | integração | ✅ |
| CA-09 | `CustomerServiceImplTest.shouldUpdateDetailsKeepingCpf`, `CustomerApiIT.shouldUpdateCustomerAndRejectStaleVersionWithoutChanges` | unitário, integração | ✅ |
| CA-10 | `CustomerServiceImplTest.shouldRejectStaleVersionInactiveCustomerAndEmailOfAnotherCustomer`, `CustomerApiIT.shouldUpdateCustomerAndRejectStaleVersionWithoutChanges`, `CustomerApiIT.shouldRejectUpdateWithVersionReadBeforeAnAddressChange` | unitário, integração | ✅ |
| CA-11 | `CustomerServiceImplTest.shouldBlockUnblockAndDeactivateWithTimestamps`, `CustomerApiIT.shouldWalkThroughStatusLifecycleAndFreezeInactiveCustomer` | unitário, integração | ✅ |
| CA-12 | `CustomerServiceImplTest.shouldBlockUnblockAndDeactivateWithTimestamps`, `CustomerApiIT.shouldWalkThroughStatusLifecycleAndFreezeInactiveCustomer` | unitário, integração | ✅ |
| CA-13 | `CustomerServiceImplTest.shouldRejectInvalidStatusTransitions`, `CustomerApiIT.shouldWalkThroughStatusLifecycleAndFreezeInactiveCustomer` | unitário, integração | ✅ |
| CA-14 | `CustomerAddressServiceImplTest.shouldRejectAddressChangesWhenCustomerIsInactive`, `CustomerApiIT.shouldWalkThroughStatusLifecycleAndFreezeInactiveCustomer` | unitário, integração | ✅ |
| CA-15 | `CustomerAddressServiceImplTest.shouldMakeOnlyTheFirstAddressDefault`, `CustomerApiIT.shouldManageAddressesKeepingSingleDefault` | unitário, integração | ✅ |
| CA-16 | `CustomerAddressServiceImplTest.shouldKeepASingleDefaultWhenSwitching`, `CustomerApiIT.shouldManageAddressesKeepingSingleDefault`, `CustomerConcurrencyIT.shouldKeepExactlyOneDefaultUnderConcurrentChanges` | unitário, integração | ✅ |
| CA-17 | `CustomerAddressServiceImplTest.shouldPromoteMostRecentWhenDefaultIsRemoved`, `CustomerApiIT.shouldManageAddressesKeepingSingleDefault` | unitário, integração | ✅ |
| CA-18 | `CustomerAddressServiceImplTest.shouldLimitAddressesToTen`, `CustomerApiIT.shouldRejectEleventhAddress`, `CustomerConcurrencyIT.shouldNeverExceedAddressLimitUnderConcurrentInclusions` | unitário, integração, concorrência | ✅ |
| CA-19 | `CustomerControllerTest.shouldValidateAddressFields`, `CustomerAddressServiceImplTest.shouldUpdateAddressAndRejectUnknownOne`, `CustomerApiIT.shouldNotTouchAddressOfAnotherCustomer` | fatia, unitário, integração | ✅ |
| CA-20 | `CustomerServiceImplTest.shouldReturnOrderDataWithDefaultChosenOrNoAddress`, `CustomerApiIT.shouldReturnOrderDataWithDefaultOrChosenAddress` | unitário, integração | ✅ |
| CA-21 | `CustomerServiceImplTest.shouldReturnOrderDataWithDefaultChosenOrNoAddress`, `CustomerApiIT.shouldReturnOrderDataForBlockedCustomerWithoutAddress` | unitário, integração | ✅ |

Também cobrem a feature: `ArchitectureTest` (camadas), `GlobalExceptionHandlerTest` (tradução de erros, inclusive banco fora do ar → 503), `ConstraintViolationsTest` (CHECK → campo da API), `ServiceSkeletonIT` (correlação, máscara de logs, valores rejeitados fora do log e OpenAPI comparado ao `contract.yaml`: operações, status, `required`, enums, `$ref` e campos anuláveis) e `CustomerApiIT.shouldAnswerBadRequestWhenOnlyTheDatabaseCheckRejectsTheData` (CHECK do banco → 400).

## Passos
- [x] Spec aprovada (Portão 1)
- [x] Modelo de dados aprovado (Portão 2)
- [x] Migration
- [x] Entidades, repository e specification
- [x] Service (regras de negócio) e mapper
- [x] DTOs, controller e OpenAPI
- [x] Reestruturação para camadas por tipo (ADR-025), a pedido do usuário
- [x] `./mvnw verify` verde
- [x] Coleção `.http` atualizada
- [x] Revisão `code-reviewer`: APROVADO (2026-10-13, após duas rodadas de correção)
- [x] Diagrama/ADR atualizados (ADR-025; páginas Componentes — customer-service, ER — aishac_customer e Máquina de estados — Cliente; ADR-008 e ADR-016 alinhados ao código)
- [x] Item marcado ✅ no backlog (F01-025, contrato produtor, segue como item próprio)
