# Tarefas — F01-020

## Mapeamento critério → teste
| Critério | Teste(s) | Nível | Status |
|---|---|---|---|
| CA-01 | `CustomerTest.shouldRegisterActiveCustomerWithNormalizedData`, `CustomerApiIT.shouldRegisterCustomerWithNormalizedData` | unitário, integração | ✅ |
| CA-02 | `CpfTest.shouldRejectInvalidCpf`, `CustomerApiIT.shouldRejectInvalidCpf` | unitário, integração | ✅ |
| CA-03 | `CustomerServiceTest.shouldRejectDuplicatedCpfOrEmailBeforeSaving`, `CustomerApiIT.shouldRejectDuplicatedCpfEvenFromInactiveCustomer`, `CustomerConcurrencyIT.shouldKeepCpfUniqueUnderConcurrentRegistrations` | unitário, integração, concorrência | ✅ |
| CA-04 | `ValueObjectsTest.shouldNormalizeEmailToLowerCaseWithoutSurroundingSpaces`, `CustomerApiIT.shouldRejectDuplicatedEmailIgnoringCaseOnCreateAndUpdate` | unitário, integração | ✅ |
| CA-05 | `CustomerControllerTest.shouldListEveryInvalidFieldOnCreate`, `CustomerControllerTest.shouldRequireVersionOnUpdate` | fatia | ✅ |
| CA-06 | `CustomerApiIT.shouldGetCustomerByIdOrAnswerNotFound` | integração | ✅ |
| CA-07 | `CustomerApiIT.shouldFilterAndPaginate` | integração | ✅ |
| CA-08 | `CustomerApiIT.shouldFindCustomerByCpfOrEmailForCheckout` | integração | ✅ |
| CA-09 | `CustomerTest.shouldUpdateDetailsKeepingCpf`, `CustomerApiIT.shouldUpdateCustomerAndRejectStaleVersion` | unitário, integração | ✅ |
| CA-10 | `CustomerServiceTest.shouldRejectStaleVersionWithoutSaving`, `CustomerApiIT.shouldUpdateCustomerAndRejectStaleVersion` | unitário, integração | ✅ |
| CA-11 | `CustomerTest.shouldBlockWithReasonAndUnblock` | unitário | ✅ |
| CA-12 | `CustomerTest.shouldDeactivateBlockedCustomerClearingBlockData`, `CustomerApiIT.shouldWalkThroughStatusLifecycle` | unitário, integração | ✅ |
| CA-13 | `CustomerTest.shouldRejectInvalidTransitions`, `CustomerApiIT.shouldWalkThroughStatusLifecycle` | unitário, integração | ✅ |
| CA-14 | `CustomerTest.shouldRejectChangesWhenInactive`, `CustomerApiIT.shouldWalkThroughStatusLifecycle` | unitário, integração | ✅ |
| CA-15 | `CustomerTest.shouldMakeFirstAddressDefault`, `CustomerApiIT.shouldManageAddressesKeepingSingleDefault` | unitário, integração | ✅ |
| CA-16 | `CustomerTest.shouldSwitchDefaultAddressKeepingOnlyOne`, `CustomerApiIT.shouldManageAddressesKeepingSingleDefault` | unitário, integração | ✅ |
| CA-17 | `CustomerTest.shouldPromoteMostRecentWhenDefaultIsRemoved`, `CustomerApiIT.shouldManageAddressesKeepingSingleDefault` | unitário, integração | ✅ |
| CA-18 | `CustomerTest.shouldLimitAddressesToTen`, `CustomerConcurrencyIT.shouldNeverExceedAddressLimitUnderConcurrentInclusions` | unitário, concorrência | ✅ |
| CA-19 | `CustomerControllerTest.shouldValidateAddressFields`, `CustomerApiIT.shouldNotTouchAddressOfAnotherCustomer` | fatia, integração | ✅ |
| CA-20 | `CustomerApiIT.shouldReturnOrderDataWithDefaultOrChosenAddress` | integração | ✅ |
| CA-21 | `CustomerApiIT.shouldReturnOrderDataForBlockedCustomerWithoutAddress` | integração | ✅ |

## Passos
- [x] Spec aprovada (Portão 1)
- [x] Modelo de dados aprovado (Portão 2)
- [x] Migration
- [x] Testes escritos e falhando
- [x] Domínio
- [x] Casos de uso e ports
- [x] Adapters (persistência + entidades JPA)
- [x] Controller + OpenAPI
- [x] `./mvnw verify` verde
- [x] Coleção `.http` atualizada
- [ ] Revisão `code-reviewer`: APROVADO
- [ ] Diagrama/ADR atualizados (se aplicável)
- [ ] Item marcado ✅ no backlog
