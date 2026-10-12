# Fronteiras de arquitetura

Esta rule vale para todo o repositório. Violações reprovam a revisão do `code-reviewer` e devem falhar no ArchUnit.

## Entre serviços
- Nenhum serviço importa código de outro. Nada de módulo Maven compartilhado com domínio ou DTOs.
- Comunicação só por HTTP, com contrato OpenAPI e teste de contrato.
- Nenhum serviço acessa o banco de outro.
- O frontend fala **apenas** com o `web-bff`.

## Dentro de um serviço (hexagonal leve)
```text
adapter/in/web  ──▶  application  ──▶  domain
adapter/out/*   ──▶  application (implementa ports) ──▶ domain
```
- `domain` não depende de nada (nem Spring, nem JPA, nem Jackson).
- `application` depende só de `domain` e dos próprios ports.
- `adapter` depende de `application` e `domain`; adapters não dependem uns dos outros.
- `infrastructure` contém configuração transversal; não contém regra de negócio.

## Entre módulos do sales-service
- `catalog`, `order` e `payment` só se comunicam por ports declarados no módulo consumidor.
- `catalog` não depende de `order` nem de `payment`.

## Regras ArchUnit mínimas
1. Camadas conforme o diagrama acima.
2. Classes de `..domain..` não dependem de `org.springframework..`, `jakarta.persistence..`, `tools.jackson..`, `lombok..` (exceto `@Getter`).
3. Nenhum campo anotado com `@Autowired`.
4. Controllers não acessam repositórios diretamente.
5. Sem ciclos entre pacotes de feature.
6. Isolamento entre módulos do sales-service.
