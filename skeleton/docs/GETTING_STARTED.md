# Getting Started

This guide walks you through the first steps after generating your service.

## First 5 Minutes

### 1. Clone and Build

```bash
git clone <your-repo-url>
cd <your-service-name>

# Verify build works
make build

# Run tests
make test
```

### 2. Start Local Dependencies

```bash
# Start databases, caches, etc. based on your selections
make deps-up

# Check services are healthy
docker-compose ps
```

### 3. Run the Service

```bash
# Run with local profile
make run

# Or with debug enabled (port 5005)
make run-debug
```

### 4. Verify It Works

```bash
# Health check
curl http://localhost:8081/health/readiness

# API status
curl http://localhost:8080/api/v1/status

# OpenAPI docs
open http://localhost:8080/swagger-ui.html
```

## Understanding Your Service

### Key Directories

```
src/main/java/fasti/sh/app/
├── config/          # Configuration classes
├── controller/      # REST endpoints
│   └── v1/          # Versioned API controllers
├── dto/             # Request/Response objects
├── exception/       # Custom exceptions
├── service/         # Business logic
├── audit/           # Audit logging
├── idempotency/     # Idempotency handling
├── locking/         # Distributed locking
├── outbox/          # Event outbox
├── resilience/      # Graceful degradation
├── security/        # Request signing
├── tracing/         # Baggage propagation
└── versioning/      # API deprecation
```

### Key Files

| File | Purpose |
|------|---------|
| `application.yaml` | All configuration |
| `Makefile` | Developer commands |
| `docker-compose.yaml` | Local dependencies |
| `pom.xml` | Dependencies & build |

## Common Tasks

### Add a New Endpoint

1. Create a DTO in `dto/`:

```java
public record CreateOrderRequest(
    @NotBlank String customerId,
    @NotEmpty List<OrderItem> items
) {}

public record OrderResponse(
    String id,
    String customerId,
    List<OrderItem> items,
    OrderStatus status,
    Instant createdAt
) {}
```

2. Create a Service in `service/`:

```java
@Slf4j
@Service
public class OrderService {

    private final MeterRegistry meterRegistry;
    private final Counter ordersCreated;

    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.ordersCreated = Counter.builder("orders.created")
            .register(meterRegistry);
    }

    @Timed("service.order.create")
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());
        // Business logic here
        ordersCreated.increment();
        return new OrderResponse(...);
    }
}
```

3. Create a Controller in `controller/v1/`:

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create order")
    @IdempotencyKey(ttlMinutes = 60)  // Enable idempotency
    @AuditLog(action = "CREATE_ORDER", resourceType = "ORDER")
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(order, "Order created"));
    }
}
```

### Add a Database Migration

Create a new file in `src/main/resources/db/migration/`:

```sql
-- V2__add_orders_table.sql
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     VARCHAR(255) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(10,2),
    tenant_id       VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_tenant ON orders(tenant_id);
```

Flyway will automatically apply it on startup.

### Add a Scheduled Job

```java
@Slf4j
@Component
public class CleanupJob {

    private final OutboxService outboxService;

    @Scheduled(cron = "0 0 * * * *")  // Every hour
    @DistributedLock(key = "'cleanup-job'", leaseTime = 300)
    public void cleanup() {
        log.info("Running cleanup job");
        // Your cleanup logic
    }
}
```

### Call an External Service

```java
@Service
public class PaymentService {

    private final RestClient restClient;
    private final FallbackService fallbackService;

    @CircuitBreaker(name = "payment-api")
    @Retry(name = "payment-api")
    public PaymentResult processPayment(PaymentRequest request) {
        return fallbackService.executeWithCachedFallback(
            "payment",
            () -> restClient.post()
                .uri("/payments")
                .body(request)
                .retrieve()
                .body(PaymentResult.class),
            () -> PaymentResult.pending(),  // Fallback
            Duration.ofMinutes(5)
        );
    }
}
```

### Publish an Event

```java
@Service
public class OrderService {

    private final OutboxService outboxService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Save order to database
        Order order = orderRepository.save(new Order(request));

        // Publish event via outbox (same transaction)
        outboxService.publish(
            "Order",
            order.getId(),
            "OrderCreated",
            new OrderCreatedEvent(order.getId(), order.getCustomerId())
        );

        return toResponse(order);
    }
}
```

## Next Steps

1. **Read the Patterns Guide** - `docs/PATTERNS.md`
2. **Review ADRs** - `docs/adr/` for architectural decisions
3. **Set up CI/CD** - Configure GitHub secrets
4. **Deploy to Dev** - Update ArgoCD configuration

## Getting Help

- **Slack**: #platform-help
- **Documentation**: This docs folder
- **Examples**: Check the `ExampleController` and `ExampleService`
