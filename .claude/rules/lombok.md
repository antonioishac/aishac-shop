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
| Services, controllers, configurações | `@RequiredArgsConstructor`, `@Slf4j` | `@Autowired` em campo |
| Entidade JPA | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder` (com `@Builder.Default` nos campos com valor inicial) | `@Data`, `@EqualsAndHashCode`, `@ToString` |
| DTOs | — | Qualquer Lombok: use **Records** |
| Exceções | `@Getter` | — |

## Por quê
- `@Data`/`@EqualsAndHashCode` em entidade usam campos mutáveis e relacionamentos lazy: quebram `Set` do Hibernate e disparam carregamentos inesperados. A igualdade é pelo id, escrita à mão uma vez na `BaseEntity` (compatível com proxies via `Hibernate.getClass`).
- `@ToString` em relacionamento bidirecional causa `StackOverflowError`.
