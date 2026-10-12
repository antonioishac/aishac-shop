# F01-020 — Cadastro de cliente

| Campo | Valor |
|---|---|
| Item do backlog | F01-020 (spec do marco 01.2; implementada por F01-021 a F01-026) |
| Contexto / serviço | customer-service |
| Fase | 01-base-sincrona |
| Status | Rascunho |

## 1. Objetivo
Manter o cadastro de clientes do Aishac Shop: dados pessoais, situação (ativo, bloqueado ou desativado) e endereços de entrega. O cadastro atende o checkout da loja, que identifica o cliente por e-mail ou CPF sem login (F01-064), o painel `/admin` e o sales-service, que precisa saber se o cliente pode comprar e para onde entregar (F01-024).

## 2. Regras de negócio

### Cliente
| ID | Regra |
|---|---|
| RN-01 | O cliente tem **nome completo**, **CPF**, **e-mail** e, opcionalmente, **telefone**. |
| RN-02 | O CPF é validado pelos **dígitos verificadores**. Sequências repetidas (ex.: `111.111.111-11`) são inválidas. É aceito com ou sem pontuação e guardado **só com os 11 dígitos**. |
| RN-03 | O CPF é **único** entre todos os clientes, inclusive os desativados, e **não pode ser alterado** depois do cadastro. |
| RN-04 | O e-mail é **único sem diferenciar maiúsculas e minúsculas**, entre todos os clientes, inclusive os desativados. É guardado em minúsculas e sem espaços nas pontas. |
| RN-05 | Todo cliente nasce com status **`ACTIVE`**. |
| RN-06 | Transições de status permitidas: `ACTIVE → BLOCKED` (bloquear, com motivo), `BLOCKED → ACTIVE` (desbloquear), `ACTIVE → INACTIVE` e `BLOCKED → INACTIVE` (desativar). Qualquer outra transição é recusada. |
| RN-07 | **Não existe exclusão de cliente** (ADR-016). Desativar é a forma de encerrar o cadastro; `INACTIVE` é final na fase 01. |
| RN-08 | Cliente `INACTIVE` não pode ter dados nem endereços alterados. Continua consultável, por causa do histórico de pedidos. |
| RN-09 | Atualização usa **lock otimista**: a requisição informa a `version` lida, e uma versão desatualizada é recusada. |
| RN-10 | Só clientes `ACTIVE` podem fazer pedidos. O customer-service informa o status; quem recusa o pedido é o sales-service (F01-040). |

### Endereços
| ID | Regra |
|---|---|
| RN-11 | O endereço tem: rótulo (ex.: "Casa"), CEP (8 dígitos), logradouro, número, complemento (opcional), bairro, cidade e UF (sigla válida de estado brasileiro). País fixo: Brasil. |
| RN-12 | Cada cliente tem **no máximo 10 endereços**. |
| RN-13 | Cada cliente tem **no máximo um endereço padrão**. O **primeiro** endereço cadastrado vira padrão automaticamente. |
| RN-14 | Definir outro endereço como padrão desmarca o anterior na mesma operação. |
| RN-15 | Ao remover o endereço padrão, se houver outros, o **cadastrado mais recentemente** vira o padrão. Remover endereço é exclusão física: pedidos guardam um *snapshot* do endereço de entrega. |

### Dados para pedido (consumidos pelo sales-service)
| ID | Regra |
|---|---|
| RN-16 | O sales-service consulta **dados para pedido**: id, nome, e-mail, status e o endereço de entrega. O endereço é o informado (`addressId`) ou, se omitido, o padrão. |
| RN-17 | Se o `addressId` informado não pertencer ao cliente, a resposta é "endereço não encontrado". Se não houver `addressId` nem endereço padrão, o endereço volta **nulo**, e o sales-service decide. |

### Privacidade
| ID | Regra |
|---|---|
| RN-18 | CPF e e-mail **não aparecem em logs** (máscara do ADR-023). As respostas da API trazem os dados completos: não há autenticação na fase 01 (ADR-006). |

## 3. Critérios de aceite

### CA-01 — Cadastrar cliente
- **Dado** que não existe cliente com o CPF `529.982.247-25` nem com o e-mail `ana@example.com`
- **Quando** cadastro "Ana Souza" com esse CPF e o e-mail `Ana@Example.com `
- **Então** recebo `201` com `Location`, status `ACTIVE`, `cpf` = `52998224725`, `email` = `ana@example.com` e `version` = `0`

### CA-02 — CPF inválido
- **Dado** um CPF com dígito verificador errado (`529.982.247-24`) ou repetido (`111.111.111-11`)
- **Quando** tento cadastrar
- **Então** recebo `400` com `code` = `INVALID_CPF`

### CA-03 — CPF duplicado, inclusive de cliente desativado
- **Dado** um cliente `INACTIVE` com o CPF `52998224725`
- **Quando** tento cadastrar outro cliente com `529.982.247-25`
- **Então** recebo `409` com `code` = `CPF_ALREADY_EXISTS`

### CA-04 — E-mail duplicado sem diferenciar maiúsculas
- **Dado** um cliente com e-mail `ana@example.com`
- **Quando** tento cadastrar outro com `ANA@EXAMPLE.COM`, ou alterar o e-mail de outro cliente para esse valor
- **Então** recebo `409` com `code` = `EMAIL_ALREADY_EXISTS`

### CA-05 — Campos obrigatórios e formatos
- **Dado** uma requisição sem nome, com e-mail mal formado ou com telefone fora do padrão
- **Quando** tento cadastrar ou atualizar
- **Então** recebo `400` com `code` = `VALIDATION_ERROR` e `errors[]` listando cada campo

### CA-06 — Buscar por ID
- **Dado** um cliente cadastrado
- **Quando** busco pelo ID dele
- **Então** recebo `200` com os dados, e um ID inexistente responde `404` com `CUSTOMER_NOT_FOUND`

### CA-07 — Listar com filtros e paginação
- **Dado** 25 clientes, sendo 3 com "souza" no nome e 1 `BLOCKED`
- **Quando** listo com `name=SOUZA`, depois com `status=BLOCKED` e depois sem filtro com `size=10`
- **Então** recebo os 3 clientes, depois só o bloqueado, e depois a página 0 com 10 itens e `totalElements` = `25`

### CA-08 — Identificar cliente no checkout
- **Dado** um cliente com CPF `52998224725` e e-mail `ana@example.com`
- **Quando** listo com `cpf=529.982.247-25` ou com `email=ANA@example.com`
- **Então** recebo exatamente esse cliente; um CPF ou e-mail sem cadastro devolve a lista vazia (`200`)

### CA-09 — Atualizar dados
- **Dado** um cliente `ACTIVE` com `version` = `0`
- **Quando** altero nome, e-mail e telefone informando `version` = `0`
- **Então** recebo `200` com os novos dados e `version` = `1`; o CPF não muda

### CA-10 — Conflito de versão
- **Dado** um cliente com `version` = `1`
- **Quando** envio uma atualização com `version` = `0`
- **Então** recebo `409` com `code` = `CONCURRENT_MODIFICATION`, e nada é alterado

### CA-11 — Bloquear e desbloquear
- **Dado** um cliente `ACTIVE`
- **Quando** bloqueio informando o motivo "Suspeita de fraude", e depois desbloqueio
- **Então** o status passa a `BLOCKED` (com motivo e data do bloqueio) e depois volta a `ACTIVE`

### CA-12 — Desativar
- **Dado** um cliente `ACTIVE` ou `BLOCKED`
- **Quando** desativo
- **Então** o status passa a `INACTIVE` e o cliente continua consultável por ID

### CA-13 — Transição inválida
- **Dado** um cliente `INACTIVE`
- **Quando** tento bloquear, desbloquear ou desativar
- **Então** recebo `409` com `code` = `INVALID_STATUS_TRANSITION`

### CA-14 — Cliente desativado não muda
- **Dado** um cliente `INACTIVE`
- **Quando** tento atualizar os dados ou incluir, alterar ou remover endereço
- **Então** recebo `422` com `code` = `CUSTOMER_INACTIVE`

### CA-15 — Primeiro endereço vira padrão
- **Dado** um cliente sem endereços
- **Quando** cadastro um endereço válido
- **Então** recebo `201`, e o endereço vem com `isDefault` = `true`

### CA-16 — Trocar o endereço padrão
- **Dado** um cliente com os endereços A (padrão) e B
- **Quando** defino B como padrão
- **Então** B passa a `isDefault` = `true` e A a `false`, nunca dois padrões ao mesmo tempo

### CA-17 — Remover o endereço padrão
- **Dado** um cliente com os endereços A (padrão, mais antigo), B e C (mais recente)
- **Quando** removo A
- **Então** recebo `204`, e C passa a ser o padrão

### CA-18 — Limite de endereços
- **Dado** um cliente com 10 endereços
- **Quando** tento cadastrar o 11º
- **Então** recebo `422` com `code` = `ADDRESS_LIMIT_EXCEEDED`

### CA-19 — Endereço inválido ou de outro cliente
- **Dado** um CEP com 7 dígitos ou a UF `XX`
- **Quando** tento cadastrar o endereço
- **Então** recebo `400` com `VALIDATION_ERROR`; e operar sobre um `addressId` de outro cliente responde `404` com `ADDRESS_NOT_FOUND`

### CA-20 — Dados para pedido com endereço padrão
- **Dado** um cliente `ACTIVE` com endereço padrão
- **Quando** o sales-service consulta os dados para pedido sem `addressId`
- **Então** recebe `200` com id, nome, e-mail, `status` = `ACTIVE` e o endereço padrão

### CA-21 — Dados para pedido de cliente bloqueado ou sem endereço
- **Dado** um cliente `BLOCKED`, ou um cliente `ACTIVE` sem endereços
- **Quando** o sales-service consulta os dados para pedido
- **Então** recebe `200` com o status real (`BLOCKED`) ou com `shippingAddress` = `null`; a decisão de recusar o pedido é do sales-service

## 4. Endpoints

Base: `/api/v1`. Todos aceitam e devolvem `X-Correlation-Id`.

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `POST` | `/customers` | Cadastrar cliente | 201 + `Location` | 400, 409 |
| `GET` | `/customers` | Listar paginado (filtros `name`, `email`, `cpf`, `status`) | 200 | 400 |
| `GET` | `/customers/{customerId}` | Buscar por ID | 200 | 404 |
| `PUT` | `/customers/{customerId}` | Atualizar nome, e-mail e telefone (com `version`) | 200 | 400, 404, 409, 422 |
| `POST` | `/customers/{customerId}/block` | Bloquear (com motivo) | 200 | 400, 404, 409 |
| `POST` | `/customers/{customerId}/unblock` | Desbloquear | 200 | 404, 409 |
| `POST` | `/customers/{customerId}/deactivate` | Desativar | 200 | 404, 409 |
| `POST` | `/customers/{customerId}/addresses` | Cadastrar endereço | 201 + `Location` | 400, 404, 422 |
| `GET` | `/customers/{customerId}/addresses` | Listar endereços (até 10, sem paginação) | 200 | 404 |
| `PUT` | `/customers/{customerId}/addresses/{addressId}` | Atualizar endereço | 200 | 400, 404, 422 |
| `DELETE` | `/customers/{customerId}/addresses/{addressId}` | Remover endereço | 204 | 404, 422 |
| `PUT` | `/customers/{customerId}/addresses/{addressId}/default` | Definir como padrão | 204 | 404, 422 |
| `GET` | `/customers/{customerId}/order-data` | Dados para pedido (`addressId` opcional) | 200 | 404 |

Contrato completo: [contract.yaml](contract.yaml)

## 5. Erros

| Código (`code`) | HTTP | Quando |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Campo obrigatório ausente ou formato inválido (lista em `errors[]`) |
| `INVALID_CPF` | 400 | CPF com dígito verificador errado ou sequência repetida |
| `CUSTOMER_NOT_FOUND` | 404 | ID de cliente inexistente |
| `ADDRESS_NOT_FOUND` | 404 | Endereço inexistente ou de outro cliente |
| `CPF_ALREADY_EXISTS` | 409 | CPF já cadastrado (qualquer status) |
| `EMAIL_ALREADY_EXISTS` | 409 | E-mail já cadastrado (qualquer status, sem diferenciar maiúsculas) |
| `INVALID_STATUS_TRANSITION` | 409 | Transição de status não permitida (RN-06) |
| `CONCURRENT_MODIFICATION` | 409 | `version` desatualizada (RN-09) |
| `CUSTOMER_INACTIVE` | 422 | Alteração em cliente desativado (RN-08) |
| `ADDRESS_LIMIT_EXCEEDED` | 422 | 11º endereço (RN-12) |

Todos em ProblemDetail com `code`, `correlationId` e `timestamp` (ADR-008).

## 6. Modelo de dados
> *Pendente — preenchido pela skill `modelar-dados` e aprovado no Portão 2.*

## 7. Dependências externas
- **Nenhuma chamada a outros serviços.** O customer-service é consumido pelo sales-service (`order-data`, com teste de contrato produtor no F01-025) e pelo web-bff.
- Banco `customers_db` (obrigatório): sem banco, o serviço fica *not ready* e responde `503`.
- Nenhuma configuração nova por ambiente.

## 8. Fora de escopo
- Autenticação, senha e login do cliente (F03).
- Consulta de CEP em serviço externo (ex.: ViaCEP): o endereço é digitado.
- Reativar cliente `INACTIVE` e anonimizar dados (IF-005).
- Endereço fora do Brasil.
- Histórico de alterações cadastrais (auditoria).

## 9. Perguntas em aberto
1. **Reativação:** `INACTIVE` deve ser mesmo final na fase 01, ou um admin pode reativar (`INACTIVE → ACTIVE`)?
2. **E-mail de desativado:** um cliente desativado "prende" o e-mail para sempre (RN-04)? A alternativa é liberar o e-mail na desativação, mas aí uma mesma pessoa poderia ter dois cadastros.
3. **Endereço padrão removido (RN-15):** promover o mais recente está bom, ou prefere deixar sem padrão até o cliente escolher?
4. **Telefone:** opcional, só celular ou fixo brasileiro (10 ou 11 dígitos, com DDD). Precisa de outro formato?
5. **Data de nascimento:** faz falta para algo da fase 01? Proponho não incluir (minimização de dados, LGPD).
6. **Motivo do bloqueio:** obrigatório (proposto, até 255 caracteres) e visível só no `/admin`?
