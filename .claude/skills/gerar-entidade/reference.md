# Estrutura de referência (exemplo: `Order` em `sales-service/order`)

```text
com.aishac.sales.order/
├── domain/
│   ├── Order.java                 # agregado raiz: create(), addItem(), pay(), cancel()
│   ├── OrderItem.java
│   ├── OrderStatus.java           # sealed interface + estados (padrão State)
│   ├── Money.java                 # record com BigDecimal
│   └── OrderNotFoundException.java
├── application/
│   ├── port/in/
│   │   ├── CreateOrderUseCase.java
│   │   └── CreateOrderCommand.java   # record
│   ├── port/out/
│   │   ├── LoadOrderPort.java
│   │   ├── SaveOrderPort.java
│   │   ├── CustomerPort.java          # implementado pelo adapter REST
│   │   └── ProductCatalogPort.java    # implementado pelo adapter que usa o módulo catalog
│   └── service/
│       └── CreateOrderService.java    # @Service @Transactional @RequiredArgsConstructor
└── adapter/
    ├── in/web/
    │   ├── OrderController.java
    │   ├── OrderWebMapper.java
    │   └── dto/{CreateOrderRequest,OrderResponse}.java
    └── out/
        ├── persistence/{OrderJpaEntity,OrderJpaRepository,OrderPersistenceAdapter,OrderPersistenceMapper}.java
        └── client/{CustomerRestClient,CustomerClientAdapter}.java
```

## Esqueletos

```java
// application/port/in/CreateOrderCommand.java
public record CreateOrderCommand(UUID customerId, String idempotencyKey, List<Item> items) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId);
        items = List.copyOf(items);
        if (items.isEmpty()) throw new IllegalArgumentException("order must have items");
    }
    public record Item(UUID productId, int quantity) {}
}
```

```java
// adapter/out/persistence/OrderJpaEntity.java
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderJpaEntity {
    @Id private UUID id;
    @Version private long version;
    // ...
    @Override public boolean equals(Object o) { /* baseado no id, compatível com proxy */ }
    @Override public int hashCode() { return getClass().hashCode(); }
}
```
