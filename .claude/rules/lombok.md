---
paths:
  - "services/**/*.java"
  - "services/**/pom.xml"
  - "services/**/lombok.config"
---

# Lombok

## Configuração obrigatória
- Annotation processors **nesta ordem** no `maven-compiler-plugin`: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`. Sem o binding, o MapStruct gera mapeamentos vazios.
- `lombok.config` na raiz de cada serviço:
  ```properties
  config.stopBubbling = true
  lombok.addLombokGeneratedAnnotation = true
  ```
  (exclui código gerado da cobertura JaCoCo).

## Onde usar

| Local | Permitido | Proibido |
|---|---|---|
| Casos de uso, adapters, configurações | `@RequiredArgsConstructor`, `@Slf4j` | `@Autowired` em campo |
| Entidade JPA | `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | `@Data`, `@EqualsAndHashCode`, `@ToString` sem exclusões, `@Setter` na classe |
| DTOs | — | Qualquer Lombok: use **Records** |
| Modelo de domínio | `@Getter` | `@Setter` (estado muda só por métodos de negócio) |
| Builders | `@Builder` em objetos complexos e em *test data builders* | `@Builder` em Record só para "ter builder" |

## Por quê
- `@Data`/`@EqualsAndHashCode` em entidade usam campos mutáveis e relacionamentos lazy: quebram `Set` do Hibernate e disparam carregamentos inesperados.
- `@ToString` em relacionamento bidirecional causa `StackOverflowError`.
- `equals`/`hashCode` de entidade: implemente à mão, baseado no ID, com `getClass()` compatível com proxies do Hibernate.
