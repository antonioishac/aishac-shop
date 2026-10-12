# ADR-003 — Lombok mantido, com regras de uso

| Campo | Valor |
|---|---|
| Status | Aceita (ajustada pelo ADR-025: entidades usam `@Setter` e `@SuperBuilder`) |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | D03 · rule `lombok` |

## Contexto
Records cobrem DTOs e Value Objects, mas casos de uso, adapters e entidades JPA ainda têm boilerplate (construtores, loggers, getters). O Lombok resolve isso, porém usos como `@Data` em entidade JPA causam bugs conhecidos, e ele precisa conviver com o MapStruct.

## Decisão
Manter o Lombok com uso restrito: `@RequiredArgsConstructor`, `@Slf4j` e `@Getter`. Em entidade JPA são **proibidos** `@Data`, `@EqualsAndHashCode` e `@Setter` de classe. DTOs são **Records**, sem Lombok. Os annotation processors seguem a ordem `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`. O `lombok.config` liga `addLombokGeneratedAnnotation`, o que exclui o código gerado da cobertura.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Sem Lombok (Records + código à mão) | Nenhuma geração de código na compilação | Muito boilerplate em entidades e serviços |
| Lombok liberado | Produtividade | `equals`/`hashCode` e `toString` perigosos em entidades |
| **Lombok com regras** | Menos código, riscos controlados | Exige disciplina e revisão |

## Consequências
- **Positivas:** classes enxutas; cobertura não distorcida por código gerado.
- **Negativas / custos:** dependência de annotation processing; regras verificadas na revisão (injeção por campo também pelo ArchUnit).
- **O que observar:** anotações proibidas aparecendo em PRs; problemas de compatibilidade a cada nova versão do Java.
