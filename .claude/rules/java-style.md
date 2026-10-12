---
paths:
  - "services/**/*.java"
---

# Estilo Java (Java 25 / Spring Boot 4)

## Linguagem moderna
- DTOs são **Records** (`dto/`). Normalização simples de entrada (strip, remover hífen) no construtor compacto; regra de negócio, nunca.
- Use `switch` expressions e pattern matching no lugar de cadeias de `if`.
- `var` só quando o tipo é óbvio no lado direito.
- Virtual Threads habilitadas (`spring.threads.virtual.enabled=true`). Para fan-out, `StructuredTaskScope`. Nunca crie pools de threads manuais para I/O.
- Nunca chame `LocalDateTime.now()`/`Instant.now()` direto: injete `java.time.Clock` (bean em `config/ClockConfiguration`).
- Use `Optional` só como retorno; nunca como campo ou parâmetro.
- Sem `package-info.java` e sem anotações de null-safety (JSpecify): o projeto não as usa (ADR-025).

## Camadas (rule `architecture-boundaries`)
- **Controller:** `public class`, `@RestController` + `@RequestMapping("/api/v1/<recurso>")`, injeta só a **interface** do service. Cada método: `@Valid` no body, uma chamada ao service, retorno do DTO (ou `ResponseEntity` para 201/204). Listagens recebem `Pageable` (`@PageableDefault`, `@ParameterObject`) e devolvem `PageResponse.of(page)`.
- **Service:** interface `XxxService` em `service/`; implementação `XxxServiceImpl` package-private em `service/impl/`, com `@Service`, `@RequiredArgsConstructor` e `@Transactional` por método (`readOnly = true` em consultas). Regras de negócio aqui; violações lançam `BusinessException(ErrorCode)` ou `NotFoundException(ErrorCode, id)`.
- **Repository:** `JpaRepository` + `JpaSpecificationExecutor`; filtros dinâmicos em `specification/XxxSpecifications`. Consultas com coleção usam `@EntityGraph` ou `JOIN FETCH` (sem N+1).
- **Mapper:** interface MapStruct em `mapper/` com `@Mapper(componentModel = "spring")`.
- **Exception:** cada serviço tem `ErrorCode` (código, mensagem padrão, status HTTP), `CommonErrorCode`, um catálogo por contexto (ex.: `CustomerErrorCode`) e o `GlobalExceptionHandler` (`@RestControllerAdvice` + `@ExceptionHandler`).

## Spring
- **Injeção somente por construtor** (`@RequiredArgsConstructor` com dependências `final`). `@Autowired` em campo é proibido.
- `@Transactional` apenas na camada service.
- Clientes HTTP como **HTTP Interfaces** (`@HttpExchange`) sobre `RestClient`.
- Jackson 3 (`tools.jackson.*`) é o padrão do Boot 4: não importe `com.fasterxml.jackson.databind` sem necessidade.
- Configuração tipada com `@ConfigurationProperties` em Records; nada de `@Value` espalhado.

## Dinheiro
- `BigDecimal` sempre; escala 2; `RoundingMode.HALF_EVEN`. Nunca `double`/`float`.

## Padrões GoF esperados
State (status do pedido), Strategy (pagamento), Factory/Builder (criação de pedido). Aplique padrão quando resolve um problema real; não por decoração.

## Logs
- `@Slf4j`; logs estruturados JSON. Nunca logue CPF, e-mail completo, tokens ou senhas.
- Mensagens em inglês, com contexto em campos (ID do pedido, ID de correlação), não concatenado em texto.
