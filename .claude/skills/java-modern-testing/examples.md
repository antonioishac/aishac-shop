# Exemplos

## Unitário de domínio
```java
class OrderTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldNotAllowPaymentOfCancelledOrder() {
        // given
        var order = anOrder().withStatus(CANCELLED).build();
        // when / then
        assertThatThrownBy(() -> order.pay(clock))
            .isInstanceOf(InvalidOrderTransitionException.class)
            .hasMessageContaining("CANCELLED");
    }
}
```

## Base de integração com Testcontainers
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:<versao-fixada>"))
        .withEnv("LOCALSTACK_AUTH_TOKEN", System.getenv("LOCALSTACK_AUTH_TOKEN"));

    @DynamicPropertySource
    static void awsProperties(DynamicPropertyRegistry registry) {
        registry.add("aishac.storage.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("aishac.storage.region", localstack::getRegion);
    }
}
```
> Versões de imagem e de classes do Testcontainers 2.x são confirmadas na criação do esqueleto (F01-012).

## Concorrência com Virtual Threads — último item em estoque
```java
@Test
void onlyOneOrderShouldWinTheLastItem() throws Exception {
    var product = givenProductWithStock(1);
    int attempts = 100;
    var start = new CountDownLatch(1);
    var results = new ConcurrentLinkedQueue<Integer>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                start.await();
                results.add(createOrder(product.id(), 1, UUID.randomUUID().toString()).statusCode());
                return null;
            });
        }
        start.countDown();
    } // close() aguarda todas as tarefas

    assertThat(results).filteredOn(s -> s == 201).hasSize(1);
    assertThat(results).filteredOn(s -> s == 409).hasSize(attempts - 1);
    assertThat(stockOf(product.id())).isZero();
}
```

## ArchUnit
```java
@AnalyzeClasses(packages = "com.aishac.sales", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "tools.jackson..");

    @ArchTest
    static final ArchRule noFieldInjection = noFields()
        .should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule catalogDoesNotKnowOrder = noClasses()
        .that().resideInAPackage("..sales.catalog..")
        .should().dependOnClassesThat().resideInAnyPackage("..sales.order..", "..sales.payment..");
}
```
