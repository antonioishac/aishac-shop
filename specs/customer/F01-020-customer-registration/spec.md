# F01-020 — Cadastro de cliente

| Campo | Valor |
|---|---|
| Item do backlog | F01-020 (spec do marco 01.2; implementada por F01-021 a F01-026) |
| Contexto / serviço | customer-service |
| Fase | 01-base-sincrona |
| Status | Aprovada em 2026-10-12 (Portão 1) |

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

Códigos transversais do serviço, que podem aparecer em qualquer endpoint: `MALFORMED_REQUEST` (400, JSON inválido), `ROUTE_NOT_FOUND` (404), `METHOD_NOT_ALLOWED` (405), `DATA_INTEGRITY_VIOLATION` (409) e `INTERNAL_ERROR` (500). Um valor rejeitado só pelo CHECK do banco responde `VALIDATION_ERROR` (400).

## 6. Modelo de dados

> **Status:** aprovado em 2026-10-12 (Portão 2). Banco `aishac_customer` (PostgreSQL 18), dono `customer_app`. Migrations: F01-021.

### Visão geral

```text
customers 1──N customer_addresses   (FK; endereços fazem parte do agregado Customer)
customers ┄┄ aishac_sales.orders.customer_id   (lógico, outro serviço, sem FK — ADR-007)
```

`Customer` é o agregado raiz; `customer_addresses` só muda através dele. Por isso **só `customers` tem `version`**, e qualquer mudança de endereço incrementa a versão do cliente (ver Concorrência).

### Tabela `customers`

| Coluna | Tipo | Nulo | Default | Regra/constraint | Origem |
|---|---|---|---|---|---|
| `id` | `UUID` | não | `uuidv7()` | `pk_customers`; gerado pela aplicação (UUID v7), default só para inserts manuais | ADR-007 |
| `full_name` | `VARCHAR(150)` | não | — | `ck_customers_full_name_length`: 3 a 150 caracteres sem espaços nas pontas | RN-01, CA-05 |
| `cpf` | `CHAR(11)` | não | — | `ck_customers_cpf_format` (11 dígitos); `uk_customers_cpf` | RN-02, RN-03, CA-03 |
| `email` | `VARCHAR(254)` | não | — | `ck_customers_email_normalized` (minúsculas, sem espaços nas pontas); `uk_customers_email` | RN-04, CA-04 |
| `phone` | `VARCHAR(11)` | sim | — | `ck_customers_phone_format` (10 ou 11 dígitos) | Decisão 4 |
| `status` | `VARCHAR(20)` | não | `'ACTIVE'` | `ck_customers_status` (`ACTIVE`, `BLOCKED`, `INACTIVE`) | RN-05, RN-06 |
| `block_reason` | `VARCHAR(255)` | sim | — | Preenchido se e somente se `BLOCKED` (`ck_customers_block_data`) | RN-06, Decisão 6 |
| `blocked_at` | `TIMESTAMPTZ` | sim | — | Idem | CA-11 |
| `deactivated_at` | `TIMESTAMPTZ` | sim | — | Preenchido se e somente se `INACTIVE` (`ck_customers_deactivation_data`) | CA-12 |
| `version` | `BIGINT` | não | `0` | Lock otimista do agregado (`@Version`) | RN-09, CA-10 |
| `created_at` | `TIMESTAMPTZ` | não | — | Vindo do `Clock` da aplicação | — |
| `updated_at` | `TIMESTAMPTZ` | não | — | Idem | — |

> Ao desativar um cliente bloqueado, `block_reason` e `blocked_at` são limpos: o histórico de bloqueio não é requisito da fase 01 (auditoria está fora de escopo).

### Tabela `customer_addresses`

| Coluna | Tipo | Nulo | Default | Regra/constraint | Origem |
|---|---|---|---|---|---|
| `id` | `UUID` | não | `uuidv7()` | `pk_customer_addresses` | — |
| `customer_id` | `UUID` | não | — | `fk_customer_addresses_customer` → `customers(id)`, `ON DELETE RESTRICT` | RN-11 |
| `label` | `VARCHAR(40)` | não | — | Não vazio (`ck_customer_addresses_label_not_blank`) | RN-11 |
| `zip_code` | `CHAR(8)` | não | — | `ck_customer_addresses_zip_code_format` (8 dígitos) | RN-11, CA-19 |
| `street` | `VARCHAR(150)` | não | — | — | RN-11 |
| `number` | `VARCHAR(10)` | não | — | Texto: aceita `S/N` | RN-11 |
| `complement` | `VARCHAR(60)` | sim | — | — | RN-11 |
| `district` | `VARCHAR(80)` | não | — | — | RN-11 |
| `city` | `VARCHAR(80)` | não | — | — | RN-11 |
| `state` | `CHAR(2)` | não | — | `ck_customer_addresses_state` (27 UFs) | RN-11, CA-19 |
| `country` | `CHAR(2)` | não | `'BR'` | `ck_customer_addresses_country` (`= 'BR'`) | RN-11 |
| `is_default` | `BOOLEAN` | não | `false` | No máximo um `true` por cliente (`uk_customer_addresses_single_default`) | RN-13, RN-14, CA-16 |
| `created_at` | `TIMESTAMPTZ` | não | — | Define o "mais recente" da RN-15 | RN-15, CA-17 |
| `updated_at` | `TIMESTAMPTZ` | não | — | — | — |

### Constraints

| Nome | Tipo | Regra |
|---|---|---|
| `pk_customers`, `pk_customer_addresses` | PK | — |
| `uk_customers_cpf` | UNIQUE | CPF único em qualquer status (RN-03) |
| `uk_customers_email` | UNIQUE | E-mail único em qualquer status; combinado com `ck_customers_email_normalized`, a unicidade ignora maiúsculas (RN-04) |
| `uk_customer_addresses_single_default` | EXCLUDE (btree, `=`) `WHERE (is_default)`, **`DEFERRABLE INITIALLY DEFERRED`** | Um padrão por cliente, verificado no `COMMIT`. Assim, desmarcar A e marcar B na mesma transação funciona em qualquer ordem de gravação (RN-14). Um índice único parcial seria verificado linha a linha e poderia falhar conforme a ordem do *flush* do Hibernate |
| `fk_customer_addresses_customer` | FK | `ON DELETE RESTRICT`: cliente nunca é excluído (RN-07) |
| `ck_customers_*`, `ck_customer_addresses_*` | CHECK | Formatos, domínio de status e UF, e consistência entre status e datas, como nas tabelas acima |

### Índices e consultas atendidas

| Índice | Consulta que atende |
|---|---|
| `uk_customers_cpf` (implícito da UNIQUE) | `GET /customers?cpf=` (checkout, CA-08) e verificação de duplicidade |
| `uk_customers_email` (implícito da UNIQUE) | `GET /customers?email=` (checkout, CA-08) e verificação de duplicidade |
| `idx_customers_created_at` (`created_at DESC`) | Listagem sem filtro, ordenação padrão `createdAt,desc` com paginação (CA-07) |
| `idx_customers_status_created_at` (`status, created_at DESC`) | `GET /customers?status=` com a ordenação padrão (CA-07) |
| `idx_customers_full_name_trgm` (GIN, `lower(full_name) gin_trgm_ops`) | `GET /customers?name=` "contém, sem diferenciar maiúsculas": `lower(full_name) LIKE '%' \|\| lower(:name) \|\| '%'` (CA-07). Requer a extensão `pg_trgm` (confiável, criada pelo dono do banco) |
| `idx_customer_addresses_customer_created` (`customer_id, created_at DESC`) | Índice da FK; listar endereços do cliente; achar o "mais recente" na RN-15; contar endereços (RN-12) |

Sem índice para `sort=fullName`: a ordenação por nome com filtro já reduz o conjunto, e o volume da fase 01 não justifica. Busca ignorando acentos (`unaccent`) fica fora de escopo.

### Concorrência

- **Dados do cliente e status:** `UPDATE ... WHERE id = :id AND version = :version`, via `@Version`. Zero linhas afetadas vira `CONCURRENT_MODIFICATION` (CA-10).
- **Endereços** (atualizado na implementação, garantia mais forte que a aprovada no Portão 2): toda operação de endereço carrega o cliente com **trava pessimista e incremento de versão** (`PESSIMISTIC_FORCE_INCREMENT`). Operações simultâneas no mesmo cliente passam a ser sequenciais: duas inclusões no 10º endereço resultam em uma aceita e outra com `ADDRESS_LIMIT_EXCEEDED` (RN-12). A `version` lida antes de uma operação de endereço fica desatualizada, e um `PUT` com ela recebe `CONCURRENT_MODIFICATION`. O limite é garantido pelo service com a trava, não por CHECK.
- **CPF e e-mail duplicados em cadastros simultâneos:** a verificação prévia é só para mensagem amigável. A garantia é a UNIQUE, e a violação de `uk_customers_cpf` ou `uk_customers_email` é traduzida para `CPF_ALREADY_EXISTS` ou `EMAIL_ALREADY_EXISTS`.
- **Endereço padrão:** a *exclusion constraint* adiável impede dois padrões mesmo com transações concorrentes.

### DDL proposto (não é a migration)

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE customers (
    id             UUID         NOT NULL DEFAULT uuidv7(),
    full_name      VARCHAR(150) NOT NULL,
    cpf            CHAR(11)     NOT NULL,
    email          VARCHAR(254) NOT NULL,
    phone          VARCHAR(11),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    block_reason   VARCHAR(255),
    blocked_at     TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_cpf UNIQUE (cpf),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT ck_customers_full_name_length
        CHECK (full_name = btrim(full_name) AND char_length(full_name) BETWEEN 3 AND 150),
    CONSTRAINT ck_customers_cpf_format CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_customers_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_customers_phone_format CHECK (phone ~ '^[0-9]{10,11}$'),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE')),
    CONSTRAINT ck_customers_block_data CHECK (
        (status = 'BLOCKED') = (block_reason IS NOT NULL AND blocked_at IS NOT NULL)
        AND (block_reason IS NULL) = (blocked_at IS NULL)),
    CONSTRAINT ck_customers_deactivation_data CHECK ((status = 'INACTIVE') = (deactivated_at IS NOT NULL))
);

CREATE INDEX idx_customers_created_at ON customers (created_at DESC);
CREATE INDEX idx_customers_status_created_at ON customers (status, created_at DESC);
CREATE INDEX idx_customers_full_name_trgm ON customers USING gin (lower(full_name) gin_trgm_ops);

CREATE TABLE customer_addresses (
    id          UUID         NOT NULL DEFAULT uuidv7(),
    customer_id UUID         NOT NULL,
    label       VARCHAR(40)  NOT NULL,
    zip_code    CHAR(8)      NOT NULL,
    street      VARCHAR(150) NOT NULL,
    number      VARCHAR(10)  NOT NULL,
    complement  VARCHAR(60),
    district    VARCHAR(80)  NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    state       CHAR(2)      NOT NULL,
    country     CHAR(2)      NOT NULL DEFAULT 'BR',
    is_default  BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT uk_customer_addresses_single_default
        EXCLUDE USING btree (customer_id WITH =) WHERE (is_default)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_customer_addresses_label_not_blank CHECK (btrim(label) <> ''),
    CONSTRAINT ck_customer_addresses_zip_code_format CHECK (zip_code ~ '^[0-9]{8}$'),
    CONSTRAINT ck_customer_addresses_state CHECK (state IN (
        'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA',
        'PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    CONSTRAINT ck_customer_addresses_country CHECK (country = 'BR')
);

CREATE INDEX idx_customer_addresses_customer_created ON customer_addresses (customer_id, created_at DESC);
```

Separação prevista nas migrations (F01-021): `V1__create_customers.sql` (extensão, tabela e índices) e `V2__create_customer_addresses.sql`.

## 7. Dependências externas
- **Nenhuma chamada a outros serviços.** O customer-service é consumido pelo sales-service (`order-data`, com teste de contrato produtor no F01-025) e pelo web-bff.
- Banco `aishac_customer` (obrigatório): sem banco, o serviço fica *not ready* e responde `503`.
- Nenhuma configuração nova por ambiente.

## 8. Fora de escopo
- Autenticação, senha e login do cliente (F03).
- Consulta de CEP em serviço externo (ex.: ViaCEP): o endereço é digitado.
- Reativar cliente `INACTIVE` e anonimizar dados (IF-005).
- Endereço fora do Brasil.
- Histórico de alterações cadastrais (auditoria).

## 9. Decisões do Portão 1 (2026-10-12)
As perguntas em aberto foram respondidas com as propostas da spec:

| # | Pergunta | Decisão |
|---|---|---|
| 1 | Reativação de `INACTIVE` | Não existe na fase 01: `INACTIVE` é final (RN-07) |
| 2 | E-mail de cliente desativado | Continua reservado; um e-mail nunca identifica dois cadastros (RN-04) |
| 3 | Remoção do endereço padrão | O cadastrado mais recentemente vira padrão (RN-15) |
| 4 | Telefone | Opcional, brasileiro, com DDD, 10 ou 11 dígitos |
| 5 | Data de nascimento | Não incluída (minimização de dados, LGPD) |
| 6 | Motivo do bloqueio | Obrigatório, 3 a 255 caracteres, exibido no `/admin` |
