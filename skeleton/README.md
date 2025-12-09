# ${{values.name}}

${{values.description | default("Java Spring Boot service for fast-ish platform", true)}}

[![Java](https://img.shields.io/badge/Java-${{values.javaVersion | default("21", true)}}-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)](https://spring.io/projects/spring-boot)
[![Datadog](https://img.shields.io/badge/Datadog-APM-purple)](https://www.datadoghq.com/)
{%- if values.aiClient != "none" %}
[![AI](https://img.shields.io/badge/AI-{%- if values.aiClient == "spring-ai" %}Spring%20AI{%- elif values.aiClient == "openai" %}OpenAI{%- elif values.aiClient == "anthropic" %}Anthropic{%- else %}Bedrock{%- endif %}-blue)](https://spring.io/projects/spring-ai)
{%- endif %}

## Overview

This service is generated from the **Java Service Golden Path** template and includes:

- **Spring Boot 3.4** with Java ${{values.javaVersion | default("21", true)}}
- **Datadog APM** for distributed tracing and profiling
- **Structured JSON logging** for Datadog log ingestion
- **Prometheus metrics** via Micrometer
- **Health checks** with liveness, readiness, and startup probes
- **Kubernetes-ready** with HPA, PDB, and security best practices
- **Golden path patterns** including exception handling, validation, DTOs, and service layer
{%- if values.aiClient != "none" %}
- **AI/LLM integration** via {%- if values.aiClient == "spring-ai" %} Spring AI with ${{values.aiProvider}}{%- elif values.aiClient == "openai" %} OpenAI Java SDK{%- elif values.aiClient == "anthropic" %} Anthropic Java SDK{%- else %} AWS Bedrock SDK{%- endif %}
{%- endif %}

## Quick Start

### Local Development

```bash
# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run -Dspring.profiles.active=local

# Run tests
./mvnw test
```

### Docker

```bash
# Build image
docker build -t ${{values.name}}:latest .

# Run container
docker run -p 8080:8080 -p 8081:8081 \
  {%- if values.aiClient == "openai" or (values.aiClient == "spring-ai" and values.aiProvider == "openai") %}
  -e OPENAI_API_KEY=your-key \
  {%- endif %}
  {%- if values.aiClient == "anthropic" or (values.aiClient == "spring-ai" and values.aiProvider == "anthropic") %}
  -e ANTHROPIC_API_KEY=your-key \
  {%- endif %}
  ${{values.name}}:latest
```

## API Endpoints

### Core Endpoints

| Endpoint | Port | Description |
|----------|------|-------------|
| `GET /api/v1/status` | 8080 | Service status |
| `GET /api/v1/info` | 8080 | Service information |
| `GET /health/liveness` | 8081 | Kubernetes liveness probe |
| `GET /health/readiness` | 8081 | Kubernetes readiness probe |
| `GET /prometheus` | 8081 | Prometheus metrics |

### Example CRUD Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/examples` | GET | List all examples (paginated) |
| `/api/v1/examples/{id}` | GET | Get example by ID |
| `/api/v1/examples` | POST | Create new example |
| `/api/v1/examples/{id}` | PUT | Update example |
| `/api/v1/examples/{id}` | DELETE | Delete example |

{%- if values.aiClient != "none" %}

### AI Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/ai/chat` | POST | Send message to AI and get response |

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, how can you help me?"}'
```
{%- endif %}

## Golden Path Code Patterns

This service includes production-ready code patterns. See [docs/PATTERNS.md](./docs/PATTERNS.md) for detailed usage.

### Exception Handling
- `GlobalExceptionHandler` - Centralized exception handling
- `ResourceNotFoundException` - 404 errors
- `BadRequestException` - 400 errors
- `ServiceException` - 500 errors
- Consistent `ErrorResponse` format

### DTOs
- `ApiResponse<T>` - Standardized API response wrapper
- `PageResponse<T>` - Pagination response

### Service Layer
- `ExampleService` - Demonstrates custom metrics, logging, and timed methods
{%- if values.aiClient != "none" %}
- `AiService` - AI/LLM integration with proper error handling
{%- endif %}

### Enterprise Patterns

| Pattern | Usage | Example |
|---------|-------|---------|
| **Idempotency** | `@IdempotencyKey` | Prevent duplicate orders |
| **Audit Logging** | `@AuditLog` | Track user actions for compliance |
| **Distributed Lock** | `@DistributedLock` | Prevent concurrent job execution |
| **Graceful Degradation** | `FallbackService` | Handle external service failures |
| **API Deprecation** | `@ApiDeprecation` | Sunset old endpoints with headers |
| **Multi-tenancy** | `TenantContext` | Isolate customer data |
| **Outbox** | `OutboxService` | Reliable event publishing |
| **Request Signing** | `RequestSigningService` | Service-to-service auth |

### Quick Example: Idempotent Endpoint

```java
@PostMapping("/orders")
@IdempotencyKey(ttlMinutes = 60)
@AuditLog(action = "CREATE_ORDER", resourceType = "ORDER")
public ResponseEntity<ApiResponse<Order>> create(@Valid @RequestBody CreateOrderRequest request) {
    return ResponseEntity.ok(ApiResponse.success(orderService.create(request)));
}
```

## Observability

### Datadog Integration

| Feature | Description |
|---------|-------------|
| **APM Tracing** | Automatic distributed tracing via `dd-java-agent` |
| **Log Correlation** | Trace IDs injected into JSON logs |
| **Profiling** | Continuous profiling for performance analysis |
| **Runtime Metrics** | JVM and application metrics |
| **AppSec** | Application security monitoring |

#### Datadog Links

- [APM Service](https://app.datadoghq.com/apm/services/${{values.name}})
- [Logs](https://app.datadoghq.com/logs?query=service%3A${{values.name}})
- [Dashboards](https://app.datadoghq.com/dashboard/lists?q=${{values.name}})

### SLO Targets

| SLI | Target | Description |
|-----|--------|-------------|
| Availability | ${{values.sloAvailability | default("99.9", true)}}% | Service uptime |
| Latency (P99) | ${{values.sloLatencyP99 | default("500ms", true)}} | Request response time |
| Error Rate | < 0.1% | Error budget |

### Custom Metrics

The service exposes custom metrics:

```promql
# Example entities created
example_entities_created_total

# Example entities deleted
example_entities_deleted_total

# API timing (auto-generated by @Timed)
api_examples_list_seconds
api_examples_create_seconds
service_example_create_seconds
{%- if values.aiClient != "none" %}
ai_chat_seconds
{%- endif %}
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_NAME` | ${{values.name}} | Application name |
| `SERVER_PORT` | 8080 | HTTP port |
| `MANAGEMENT_PORT` | 8081 | Actuator/metrics port |
| `LOG_LEVEL` | INFO | Logging level |
| `ENVIRONMENT` | development | Environment name |
| `DD_AGENT_HOST` | datadog-agent | Datadog agent host |

{%- if values.aiClient == "openai" or (values.aiClient == "spring-ai" and values.aiProvider == "openai") %}

### OpenAI Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | - | OpenAI API key (required) |
| `OPENAI_MODEL` | gpt-4o | Model to use |
{%- endif %}

{%- if values.aiClient == "anthropic" or (values.aiClient == "spring-ai" and values.aiProvider == "anthropic") %}

### Anthropic Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `ANTHROPIC_API_KEY` | - | Anthropic API key (required) |
| `ANTHROPIC_MODEL` | claude-sonnet-4-20250514 | Model to use |
{%- endif %}

{%- if values.aiClient == "bedrock" or (values.aiClient == "spring-ai" and values.aiProvider == "bedrock") %}

### AWS Bedrock Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `AWS_REGION` | us-east-1 | AWS region |
| `BEDROCK_MODEL_ID` | anthropic.claude-3-sonnet-20240229-v1:0 | Model ID |
{%- endif %}

{%- if values.database == "aurora-postgresql" or values.database == "aurora-mysql" %}

### Database Configuration

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC connection URL |
| `DATABASE_USERNAME` | Database username |
| `DATABASE_PASSWORD` | Database password |
| `DB_POOL_SIZE` | Connection pool size (default: 10) |
{%- endif %}

{%- if values.cache == "elasticache-redis" %}

### Redis Configuration

| Variable | Description |
|----------|-------------|
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port (default: 6379) |
| `REDIS_PASSWORD` | Redis password |
| `REDIS_SSL` | Enable SSL (default: false) |
{%- endif %}

## Project Structure

```
${{values.name}}/
├── src/
│   ├── main/
│   │   ├── java/fasti/sh/app/
│   │   │   ├── Application.java
│   │   │   ├── config/
│   │   │   │   └── ObservabilityConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ApiController.java
│   │   │   │   ├── ExampleController.java
{%- if values.aiClient != "none" %}
│   │   │   │   └── AiController.java
{%- endif %}
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── PageResponse.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   └── ServiceException.java
│   │   │   ├── health/
│   │   │   │   └── ReadinessIndicator.java
│   │   │   └── service/
│   │   │       ├── ExampleService.java
{%- if values.aiClient != "none" %}
│   │   │       └── AiService.java
{%- endif %}
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── external-secret.yaml
│   ├── hpa.yaml
│   └── pdb.yaml
├── Dockerfile
├── catalog-info.yaml
├── pom.xml
└── README.md
```

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster with Datadog Agent DaemonSet
- ArgoCD for GitOps deployment
- External Secrets Operator with AWS Secrets Manager

### Deploy with ArgoCD

Add to your team's `team.yaml` in `aws-idp-gitops`:

```yaml
apps:
  - name: ${{values.name}}
    repoURL: https://github.com/${{values.owner}}/${{values.name}}
    path: k8s
    targetRevision: main
    environment: production
```

### Secrets Setup

Create a secret in AWS Secrets Manager at `${{values.owner}}/${{values.name}}` with:

```json
{
  "api_key": "your-api-key"
  {%- if values.database == "aurora-postgresql" or values.database == "aurora-mysql" %},
  "database_url": "jdbc:...",
  "database_username": "...",
  "database_password": "..."
  {%- endif %}
  {%- if values.cache == "elasticache-redis" %},
  "redis_host": "...",
  "redis_password": "..."
  {%- endif %}
  {%- if values.aiClient == "openai" or (values.aiClient == "spring-ai" and values.aiProvider == "openai") %},
  "openai_api_key": "sk-..."
  {%- endif %}
  {%- if values.aiClient == "anthropic" or (values.aiClient == "spring-ai" and values.aiProvider == "anthropic") %},
  "anthropic_api_key": "sk-ant-..."
  {%- endif %}
}
```

## Developer Commands

This service includes a `Makefile` for common operations:

```bash
make help              # Show all commands
make build             # Compile the application
make test              # Run unit tests
make test-all          # Run all tests with coverage
make run               # Run locally
make run-debug         # Run with debugger (port 5005)
make deps-up           # Start local dependencies
make deps-down         # Stop local dependencies
make lint              # Run code style checks
make security-check    # Run security scans
make quality           # Run all quality checks
make docker-build      # Build Docker image
```

## Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](./docs/GETTING_STARTED.md) | First steps, common tasks |
| [Patterns Guide](./docs/PATTERNS.md) | Enterprise patterns with examples |
| [Extending Guide](./docs/EXTENDING.md) | How to customize your service |
| [ADRs](./docs/adr/) | Architecture decisions |

## Development

### Adding Dependencies

Edit `pom.xml` to add new dependencies. Uses Spring Boot's dependency management.

### Creating New Endpoints

Add controllers in `src/main/java/fasti/sh/app/controller/`:

```java
@RestController
@RequestMapping("/api/v1/my-resource")
@RequiredArgsConstructor
public class MyController {

    private final MyService myService;

    @GetMapping("/{id}")
    @Timed(value = "api.my.get", description = "Time to get resource")
    public ResponseEntity<ApiResponse<MyEntity>> get(@PathVariable Long id) {
        MyEntity entity = myService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(entity));
    }

    @PostMapping
    @Timed(value = "api.my.create", description = "Time to create resource")
    public ResponseEntity<ApiResponse<MyEntity>> create(
            @Valid @RequestBody CreateRequest request) {
        MyEntity entity = myService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(entity));
    }
}
```

### Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Test coverage report
./mvnw jacoco:report
# View: target/site/jacoco/index.html
```

## Troubleshooting

### Common Issues

**Service not appearing in Datadog APM**
- Verify `DD_AGENT_HOST` is set correctly
- Check that the Datadog Agent pod is running
- Ensure `DD_TRACE_ENABLED=true`

{%- if values.aiClient != "none" %}

**AI endpoint returning errors**
- Verify API key is set correctly
- Check logs for detailed error messages
- Ensure model name is valid
{%- endif %}

**Health check failing**
- Check application logs: `kubectl logs -f deployment/${{values.name}}`
- Verify management port (8081) is accessible
- Check readiness dependencies

## Support

- **Owner**: ${{values.owner}}
- **Backstage**: [View in Catalog](https://backstage.yourcompany.com/catalog/default/component/${{values.name}})
- **Datadog**: [Service Dashboard](https://app.datadoghq.com/apm/services/${{values.name}})
