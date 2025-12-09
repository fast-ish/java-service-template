# Enterprise Patterns Guide

This service includes production-ready patterns for enterprise applications. This guide explains when and how to use each pattern.

## Quick Reference

| Pattern | When to Use | Annotation/Class |
|---------|-------------|------------------|
| Idempotency | POST/PUT that shouldn't duplicate | `@IdempotencyKey` |
| Audit Logging | Compliance, security events | `@AuditLog` |
| Distributed Lock | Prevent concurrent operations | `@DistributedLock` |
| Graceful Degradation | External service calls | `FallbackService` |
| API Deprecation | Sunsetting old endpoints | `@ApiDeprecation` |
| Multi-tenancy | SaaS, isolated customers | `TenantContext` |
| Outbox | Reliable event publishing | `OutboxService` |
| Request Signing | Service-to-service auth | `RequestSigningService` |

---

## Idempotency

**Problem**: Client retries can cause duplicate orders, payments, etc.

**Solution**: Use `@IdempotencyKey` to ensure operations only execute once.

### Usage

```java
@PostMapping("/orders")
@IdempotencyKey(header = "Idempotency-Key", ttlMinutes = 1440)
public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
    return ResponseEntity.ok(orderService.create(request));
}
```

### Client Usage

```bash
# Client generates unique key
curl -X POST /api/v1/orders \
  -H "Idempotency-Key: order-123-attempt-1" \
  -H "Content-Type: application/json" \
  -d '{"items": [...]}'

# Same key = same response (no duplicate order)
curl -X POST /api/v1/orders \
  -H "Idempotency-Key: order-123-attempt-1" \
  -H "Content-Type: application/json" \
  -d '{"items": [...]}'
```

### When to Use

- Payment processing
- Order creation
- Resource provisioning
- Any non-idempotent operation that clients might retry

---

## Audit Logging

**Problem**: Need compliance trail for who did what, when.

**Solution**: Use `@AuditLog` to automatically log operations.

### Usage

```java
@PostMapping("/users/{id}/permissions")
@AuditLog(
    action = "GRANT_PERMISSION",
    resourceType = "USER",
    includeRequestBody = true,
    redactFields = {"password", "ssn"}
)
public ResponseEntity<User> grantPermission(
        @PathVariable String id,
        @RequestBody PermissionRequest request) {
    return ResponseEntity.ok(userService.grantPermission(id, request));
}
```

### Logged Fields

- Timestamp, user ID, tenant ID
- Action and resource type/ID
- Request/response bodies (with redaction)
- Correlation ID for tracing
- IP address and user agent
- Duration

### When to Use

- User management (create, delete, permission changes)
- Financial transactions
- Data access (for privacy compliance)
- Configuration changes
- Security events (login, logout, failed auth)

---

## Distributed Locking

**Problem**: Multiple instances shouldn't process the same job simultaneously.

**Solution**: Use `@DistributedLock` or `DistributedLockService`.

### Annotation Usage

```java
@Scheduled(fixedRate = 60000)
@DistributedLock(
    key = "'invoice-generation'",
    leaseTime = 300,  // 5 minutes max
    waitTime = 0      // Don't wait, skip if locked
)
public void generateInvoices() {
    // Only one instance runs this
}
```

### Programmatic Usage

```java
@Service
public class OrderProcessor {

    private final DistributedLockService lockService;

    public void processOrder(String orderId) {
        lockService.executeWithLockOrThrow(
            "order-" + orderId,
            Duration.ofMinutes(5),
            () -> {
                // Process order exclusively
                return doProcess(orderId);
            }
        );
    }
}
```

### With SpEL for Dynamic Keys

```java
@DistributedLock(key = "'user-' + #userId + '-profile-update'")
public void updateProfile(String userId, ProfileRequest request) {
    // Prevents concurrent profile updates for same user
}
```

### When to Use

- Scheduled jobs that shouldn't run concurrently
- Resource-specific operations (order processing)
- Leader election
- Rate limiting per resource

---

## Graceful Degradation

**Problem**: External service failures shouldn't crash your service.

**Solution**: Use `FallbackService` for circuit breakers with cached fallbacks.

### Usage

```java
@Service
public class RecommendationService {

    private final FallbackService fallbackService;
    private final RecommendationClient client;

    public List<Product> getRecommendations(String userId) {
        return fallbackService.executeWithCachedFallback(
            "recommendations",
            () -> client.getPersonalized(userId),  // Primary
            () -> getPopularProducts(),             // Fallback
            Duration.ofHours(1)                     // Cache TTL
        );
    }

    private List<Product> getPopularProducts() {
        // Static/cached popular products as fallback
        return List.of(...);
    }
}
```

### Check Degradation Status

```java
@GetMapping("/health/degradation")
public Map<String, FallbackService.DegradationStatus> getDegradationStatus() {
    return fallbackService.getDegradationStatus();
}
```

### When to Use

- ML/AI service calls
- Third-party APIs (payment, shipping)
- Non-critical enrichment data
- Any external dependency

---

## API Deprecation

**Problem**: Need to sunset old APIs without breaking clients.

**Solution**: Use `@ApiDeprecation` to add sunset headers and track usage.

### Usage

```java
@GetMapping("/users")
@ApiDeprecation(
    since = "2024-01-01",
    sunset = "2024-06-01",
    successor = "/api/v2/users",
    message = "Use v2 API for cursor-based pagination"
)
public ResponseEntity<List<User>> listUsersV1() {
    // Old endpoint - will be removed
}
```

### Response Headers

```http
HTTP/1.1 200 OK
Deprecation: @1704067200
Sunset: Sat, 01 Jun 2024 00:00:00 GMT
Link: </api/v2/users>; rel="successor-version"
X-Deprecation-Notice: Use v2 API for cursor-based pagination
Warning: 299 - "Deprecated API: sunset on 2024-06-01"
```

### When to Use

- Breaking API changes
- Endpoint consolidation
- Technology migration

---

## Multi-Tenancy

**Problem**: SaaS app needs to isolate customer data.

**Solution**: Use `TenantContext` for tenant-aware operations.

### Filter Setup (Automatic)

Tenants are identified via:
1. `X-Tenant-ID` header
2. Subdomain (tenant.example.com)
3. Path (`/api/v1/tenants/{tenantId}/...`)

### Usage in Services

```java
@Service
public class OrderService {

    public List<Order> findAll() {
        String tenantId = TenantContext.getTenantId();
        return orderRepository.findByTenantId(tenantId);
    }

    public Order create(CreateOrderRequest request) {
        Order order = new Order(request);
        order.setTenantId(TenantContext.getTenantId());
        return orderRepository.save(order);
    }
}
```

### JPA Entity

```java
@Entity
@Table(name = "orders")
public class Order {

    @Column(name = "tenant_id")
    private String tenantId;

    @PrePersist
    public void prePersist() {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
    }
}
```

### Enable in Configuration

```yaml
multitenancy:
  enabled: true
  require-tenant: true  # 400 if no tenant
```

---

## Transactional Outbox

**Problem**: Need reliable event publishing with database transactions.

**Solution**: Use `OutboxService` to publish events transactionally.

### Usage

```java
@Service
public class OrderService {

    private final OrderRepository repository;
    private final OutboxService outboxService;

    @Transactional
    public Order create(CreateOrderRequest request) {
        // 1. Save to database
        Order order = repository.save(new Order(request));

        // 2. Publish event (same transaction!)
        outboxService.publish(
            "Order",                    // Aggregate type
            order.getId().toString(),   // Aggregate ID
            "OrderCreated",             // Event type
            Map.of(                     // Payload
                "orderId", order.getId(),
                "customerId", order.getCustomerId(),
                "total", order.getTotal()
            )
        );

        return order;
    }
}
```

### Custom Event Handler

```java
@Component
public class OrderEventHandler {

    private final SqsClient sqsClient;
    private final OutboxService outboxService;

    @PostConstruct
    public void register() {
        outboxService.registerHandler("OrderCreated", this::handleOrderCreated);
    }

    private void handleOrderCreated(OutboxEvent event) {
        sqsClient.sendMessage(SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(event.payload())
            .build());
    }
}
```

### When to Use

- Event-driven architectures
- Saga patterns
- Integration with message brokers
- Any "save entity + publish event" scenario

---

## Request Signing

**Problem**: Need to verify service-to-service requests.

**Solution**: Use `RequestSigningService` for HMAC signatures.

### Outgoing Request

```java
@Service
public class PaymentClient {

    private final RestClient restClient;
    private final RequestSigningService signingService;

    public PaymentResult charge(ChargeRequest request) {
        String body = objectMapper.writeValueAsString(request);

        Map<String, String> signatureHeaders = signingService.signRequest(
            "POST",
            "/api/v1/charges",
            body,
            Map.of("X-Request-ID", UUID.randomUUID().toString())
        );

        return restClient.post()
            .uri("/api/v1/charges")
            .headers(h -> signatureHeaders.forEach(h::set))
            .body(request)
            .retrieve()
            .body(PaymentResult.class);
    }
}
```

### Verify Incoming (Filter)

```java
@Component
public class RequestSignatureFilter extends OncePerRequestFilter {

    private final RequestSigningService signingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String body = // read body

        if (!signingService.verifyRequest(
                request.getMethod(),
                request.getRequestURI(),
                body,
                extractHeaders(request))) {
            response.setStatus(401);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

### Enable in Configuration

```yaml
security:
  signing:
    enabled: true
    secret: ${REQUEST_SIGNING_SECRET}  # Shared secret
    service-id: my-service
```

---

## Best Practices Summary

1. **Idempotency**: Always use for mutations (POST, PUT, DELETE)
2. **Audit**: Log all sensitive operations, redact PII
3. **Locks**: Use short lease times, handle lock failures gracefully
4. **Fallbacks**: Always have a degraded mode for external calls
5. **Deprecation**: Give 6+ months notice, track usage
6. **Multi-tenancy**: Never trust client-provided tenant ID alone
7. **Outbox**: Use for any "save + publish" scenario
8. **Signing**: Use for all internal service-to-service calls
