---
paths:
  - "services/**/*.java"
---

# Estilo Java (Java 25 / Spring Boot 4)

## Linguagem moderna
- DTOs, comandos, consultas e Value Objects simples são **Records**. Validação no construtor compacto.
- Use `switch` expressions e pattern matching (`instanceof`, record patterns, `sealed`) no lugar de cadeias de `if`.
- Hierarquias fechadas (status, resultados, erros de domínio) como `sealed interface` + records.
- `var` só quando o tipo é óbvio no lado direito.
- Virtual Threads habilitadas (`spring.threads.virtual.enabled=true`). Para fan-out, `StructuredTaskScope`. Nunca crie pools de threads manuais para I/O.
- Nunca chame `LocalDateTime.now()`/`Instant.now()` direto: injete `java.time.Clock`.
- Use `Optional` só como retorno; nunca como campo ou parâmetro.
- Anotações de null-safety JSpecify (`@NullMarked` no `package-info.java`, `@Nullable` onde aplicável).

## Spring
- **Injeção somente por construtor** (`@RequiredArgsConstructor` em classes com dependências `final`). `@Autowired` em campo é proibido.
- `@Transactional` apenas na camada `application` (casos de uso), nunca em controller ou adapter web.
- `readOnly = true` em casos de uso de consulta.
- Clientes HTTP como **HTTP Interfaces** (`@HttpExchange`) sobre `RestClient`.
- Jackson 3 (`tools.jackson.*`) é o padrão do Boot 4: não importe `com.fasterxml.jackson.databind` sem necessidade.
- Configuração tipada com `@ConfigurationProperties` em Records; nada de `@Value` espalhado.

## Domínio
- Modelo de domínio sem anotações de framework (sem JPA, sem Spring).
- Estado muda só por métodos de negócio (`order.pay()`, `customer.block()`); sem setters públicos.
- Exceções de domínio estendem uma base `DomainException` com código do catálogo de erros.

## Dinheiro
- `BigDecimal` sempre; escala 2; `RoundingMode.HALF_EVEN`. Nunca `double`/`float`.
- Encapsule em um Value Object `Money` quando houver operações.

## Padrões GoF esperados
State (status do pedido), Strategy (pagamento), Factory/Builder (criação de pedido), Adapter (ports & adapters). Aplique padrão quando resolve um problema real; não por decoração.

## Logs
- `@Slf4j`; logs estruturados JSON. Nunca logue CPF, e-mail completo, tokens ou senhas.
- Mensagens em inglês, com contexto em campos (ID do pedido, ID de correlação), não concatenado em texto.
