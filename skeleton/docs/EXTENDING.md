# Extending Your Service

This guide shows how to customize and extend the generated service for your specific needs.

## Table of Contents

- [Adding a New Domain](#adding-a-new-domain)
- [Database Integration](#database-integration)
- [Caching Strategies](#caching-strategies)
- [Async Processing](#async-processing)
- [External API Integration](#external-api-integration)
- [Custom Metrics](#custom-metrics)
- [Feature Flags](#feature-flags)
- [Testing Patterns](#testing-patterns)

---

## Adding a New Domain

### 1. Define Your Domain Model

```java
// src/main/java/fasti/sh/app/domain/Product.java
public record Product(
    String id,
    String name,
    String description,
    BigDecimal price,
    ProductStatus status,
    String tenantId,
    Instant createdAt,
    Instant updatedAt
) {
    public enum ProductStatus {
        DRAFT, ACTIVE, DISCONTINUED
    }
}
```

### 2. Create DTOs

```java
// Request DTO with validation
public record CreateProductRequest(
    @NotBlank @Size(max = 255)
    String name,

    @Size(max = 2000)
    String description,

    @NotNull @Positive
    BigDecimal price
) {}

// Response DTO
public record ProductResponse(
    String id,
    String name,
    String description,
    BigDecimal price,
    String status,
    Instant createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.id(),
            product.name(),
            product.description(),
            product.price(),
            product.status().name(),
            product.createdAt()
        );
    }
}
```

### 3. Create Service

```java
@Slf4j
@Service
public class ProductService {

    private final ProductRepository repository;
    private final OutboxService outboxService;
    private final MeterRegistry meterRegistry;

    private final Counter productsCreated;

    public ProductService(ProductRepository repository,
                         OutboxService outboxService,
                         MeterRegistry meterRegistry) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.meterRegistry = meterRegistry;
        this.productsCreated = Counter.builder("products.created")
            .description("Products created")
            .register(meterRegistry);
    }

    @Timed("service.product.create")
    @Transactional
    public Product create(CreateProductRequest request) {
        log.info("Creating product: {}", request.name());

        Product product = new Product(
            UUID.randomUUID().toString(),
            request.name(),
            request.description(),
            request.price(),
            Product.ProductStatus.DRAFT,
            TenantContext.getTenantId(),
            Instant.now(),
            Instant.now()
        );

        Product saved = repository.save(product);

        outboxService.publish("Product", saved.id(), "ProductCreated", saved);
        productsCreated.increment();

        return saved;
    }

    @Timed("service.product.findById")
    public Product findById(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
}
```

### 4. Create Controller

```java
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create product")
    @IdempotencyKey(ttlMinutes = 60)
    @AuditLog(action = "CREATE_PRODUCT", resourceType = "PRODUCT")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(ProductResponse.from(product)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable String id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(ProductResponse.from(product)));
    }
}
```

---

## Database Integration

### JPA Entity

```java
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "tenant_id")
    private String tenantId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;  // Optimistic locking

    @PrePersist
    public void prePersist() {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
    }
}
```

### Repository with Tenant Filtering

```java
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId")
    List<ProductEntity> findAllByTenant(@Param("tenantId") String tenantId);

    default List<ProductEntity> findAllForCurrentTenant() {
        return findAllByTenant(TenantContext.getTenantId());
    }

    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id AND p.tenantId = :tenantId")
    Optional<ProductEntity> findByIdAndTenant(
        @Param("id") UUID id,
        @Param("tenantId") String tenantId);
}
```

### Migration

```sql
-- V2__create_products_table.sql
CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    tenant_id       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_price_positive CHECK (price > 0)
);

CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_name ON products(name);
```

---

## Caching Strategies

### Spring Cache with Redis

```java
@Service
@CacheConfig(cacheNames = "products")
public class ProductService {

    @Cacheable(key = "#id")
    public Product findById(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @CachePut(key = "#result.id")
    public Product create(CreateProductRequest request) {
        // Creates and returns product
    }

    @CacheEvict(key = "#id")
    public void delete(String id) {
        repository.deleteById(id);
    }

    @CacheEvict(allEntries = true)
    @Scheduled(fixedRate = 3600000)  // Hourly
    public void evictAllCache() {
        log.info("Evicting all product cache");
    }
}
```

### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(
                SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

---

## Async Processing

### Async Method

```java
@Service
public class NotificationService {

    @Async("taskExecutor")
    public CompletableFuture<Void> sendNotification(NotificationRequest request) {
        // Long-running notification logic
        return CompletableFuture.completedFuture(null);
    }
}
```

### Batch Processing

```java
@Service
public class BatchProcessor {

    private final Executor backgroundExecutor;

    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    @DistributedLock(key = "'nightly-batch'", leaseTime = 3600)
    public void processNightlyBatch() {
        List<Order> pending = orderRepository.findPending();

        // Process in parallel batches
        Lists.partition(pending, 100).forEach(batch -> {
            CompletableFuture.runAsync(() -> processBatch(batch), backgroundExecutor);
        });
    }
}
```

---

## External API Integration

### REST Client with Resilience

```java
@Service
public class PaymentGatewayClient {

    private final RestClient restClient;
    private final FallbackService fallbackService;
    private final RequestSigningService signingService;

    public PaymentGatewayClient(RestClient.Builder builder,
                               FallbackService fallbackService,
                               RequestSigningService signingService) {
        this.restClient = builder
            .baseUrl("https://api.payment-gateway.com")
            .build();
        this.fallbackService = fallbackService;
        this.signingService = signingService;
    }

    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "chargeFallback")
    @Retry(name = "payment-gateway")
    @TimeLimiter(name = "payment-gateway")
    public PaymentResult charge(ChargeRequest request) {
        String body = serialize(request);
        Map<String, String> signedHeaders = signingService.signRequest(
            "POST", "/v1/charges", body, Map.of());

        return restClient.post()
            .uri("/v1/charges")
            .headers(h -> signedHeaders.forEach(h::set))
            .body(request)
            .retrieve()
            .body(PaymentResult.class);
    }

    private PaymentResult chargeFallback(ChargeRequest request, Throwable t) {
        log.error("Payment gateway unavailable, queueing for retry", t);
        outboxService.publish("Payment", request.orderId(), "PaymentPending", request);
        return PaymentResult.pending(request.orderId());
    }
}
```

---

## Custom Metrics

### Business Metrics

```java
@Service
public class OrderMetrics {

    private final MeterRegistry registry;
    private final AtomicLong activeOrders;

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Counter
        Counter.builder("orders.total")
            .description("Total orders")
            .register(registry);

        // Gauge
        this.activeOrders = registry.gauge("orders.active",
            new AtomicLong(0));

        // Distribution Summary
        DistributionSummary.builder("order.value")
            .description("Order values")
            .baseUnit("dollars")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    public void recordOrder(Order order) {
        registry.counter("orders.total",
            "status", order.getStatus().name(),
            "region", order.getRegion()
        ).increment();

        registry.summary("order.value").record(order.getTotal().doubleValue());
    }
}
```

---

## Feature Flags

### Define Features

```java
public enum Features implements Feature {

    @EnabledByDefault
    @Label("Enable new checkout flow")
    NEW_CHECKOUT_FLOW,

    @Label("Enable AI recommendations")
    AI_RECOMMENDATIONS,

    @Label("Enable bulk operations")
    BULK_OPERATIONS;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
```

### Use in Code

```java
@Service
public class CheckoutService {

    public CheckoutResult checkout(CheckoutRequest request) {
        if (Features.NEW_CHECKOUT_FLOW.isActive()) {
            return newCheckoutFlow(request);
        }
        return legacyCheckoutFlow(request);
    }
}
```

### Use in Controller

```java
@GetMapping("/recommendations")
public ResponseEntity<?> getRecommendations(@RequestParam String userId) {
    if (!Features.AI_RECOMMENDATIONS.isActive()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error("Feature not available"));
    }
    return ResponseEntity.ok(recommendationService.get(userId));
}
```

---

## Testing Patterns

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private ProductService service;

    @Test
    void create_shouldSaveAndPublishEvent() {
        // Given
        var request = new CreateProductRequest("Test", "Desc", BigDecimal.TEN);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        Product result = service.create(request);

        // Then
        assertThat(result.name()).isEqualTo("Test");
        verify(outboxService).publish(eq("Product"), any(), eq("ProductCreated"), any());
    }
}
```

### Integration Test

```java
class ProductControllerIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createProduct_shouldReturn201() {
        var request = new CreateProductRequest("Test", "Desc", BigDecimal.TEN);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/products",
            request,
            ApiResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
```

### Contract Test

See `src/test/java/fasti/sh/app/contract/` for Pact examples.

---

## Common Customizations

### Change Base Package

Update in:
1. `pom.xml` - groupId
2. All Java files - package declarations
3. `application.yaml` - component scan paths
4. `checkstyle.xml` - if package-specific rules

### Add New Profile

```yaml
# application.yaml
---
spring:
  config:
    activate:
      on-profile: staging

# Staging-specific config
logging:
  level:
    root: DEBUG
```

### Override for Specific Environment

```bash
# Use environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://staging-db:5432/myapp
export LOG_LEVEL=DEBUG

# Or Java properties
java -jar app.jar --spring.profiles.active=staging
```
