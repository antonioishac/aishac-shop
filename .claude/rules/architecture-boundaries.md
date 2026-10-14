# Fronteiras de arquitetura

Esta rule vale para todo o repositório. Violações reprovam a revisão do `code-reviewer` e devem falhar no ArchUnit (ADR-025).

## Entre serviços
- Nenhum serviço importa código de outro. Nada de módulo Maven compartilhado com entidades ou DTOs.
- Comunicação só por HTTP, com contrato OpenAPI e teste de contrato.
- Nenhum serviço acessa o banco de outro.
- O frontend fala **apenas** com o `web-bff`.

## Dentro de um serviço (camadas por tipo)

```text
com.aishac.<servico>/
├── config/         # @Configuration, filtros, OpenAPI, Clock, auditoria, logs
├── controller/     # @RestController: recebe, valida (@Valid) e delega ao service
├── dto/            # Records de request/response e PageResponse
├── entity/         # entidades JPA (Hibernate) e enums persistidos
├── exception/      # ErrorCode + catálogos, exceções, @RestControllerAdvice
├── mapper/         # interfaces MapStruct
├── repository/     # Spring Data JPA
├── service/        # interfaces de serviço (+ helpers de regra)
│   └── impl/       # implementações (package-private, @Service, @Transactional)
└── specification/  # filtros dinâmicos (JPA Specification)
```

```text
controller ──▶ service ──▶ repository / specification / mapper
     │             │
     └── dto ◀─────┘── entity, exception
```

- **Controller sem regra de negócio:** só recebe a requisição, valida o DTO com `@Valid`, chama **um** método do service e devolve a resposta. Não acessa repository, mapper nem specification.
- **Service concentra as regras de negócio**, a transação (`@Transactional`) e a orquestração. É o único que usa repository, specification e mapper. Interface em `service/`, implementação package-private em `service/impl/`.
- **Entity** é só mapeamento JPA (com Lombok); não depende de service, controller, dto nem repository.
- **DTO** não depende de service, repository nem mapper. Nunca exponha entidade na API.
- **Mapper** (MapStruct) converte entidade → resposta e copia campos simples de request → entidade. Lógica condicional fica no service.

## Entre módulos do sales-service (ADR-026)
- Cada módulo é um pacote com as camadas dentro: `com.aishac.sales.<modulo>.{config,controller,dto,entity,exception,mapper,repository,service,specification}`. Na raiz ficam só os transversais: `config/`, `exception/`, `entity/BaseEntity` e `dto/{PageResponse,ProblemResponse}`.
- Outros módulos enxergam do catálogo só as interfaces `catalog.service.*Service`, `catalog.dto` e `catalog.exception`.
- `catalog` não depende de `order` nem de `payment`.
- Um módulo usa outro **somente pelas interfaces de service** do outro (nunca repository, entity, mapper ou specification alheios), trafegando DTOs ou tipos simples.
- Sem FK entre tabelas de módulos diferentes.

## Regras ArchUnit mínimas (`ArchitectureTest` de cada serviço)
1. Controller não é acessado por ninguém; service só por controller; repository, specification e mapper só por service.
2. Entity e DTO não dependem de controller, service, repository nem mapper.
3. `@RestController` só em `controller`.
4. Implementações em `service.impl` não são públicas.
5. Nenhum campo anotado com `@Autowired`.
6. Sem ciclos entre pacotes.
7. sales-service: isolamento entre módulos conforme acima.
