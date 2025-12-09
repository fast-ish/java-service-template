# Java Service Golden Path Template

> The recommended way to build Java services at our organization.

[![Backstage](https://img.shields.io/badge/Backstage-Template-blue)](https://backstage.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Internal-red)]()

## What's Included

| Category | Features |
|----------|----------|
| **Core** | Spring Boot 3.4, Java 21, Gradle/Maven |
| **Observability** | OpenTelemetry + Grafana, Prometheus metrics, structured logging |
| **Resilience** | Circuit breakers, retries, rate limiting, graceful degradation |
| **Security** | OWASP scanning, secrets detection, security headers, request signing |
| **Data** | PostgreSQL, MySQL, DynamoDB, Redis, Flyway migrations |
| **Messaging** | SQS, SNS, transactional outbox |
| **AI** | Spring AI, LangChain4j, OpenAI, Anthropic, Bedrock |
| **Enterprise** | Multi-tenancy, idempotency, audit logging, distributed locking |
| **Testing** | JUnit 5, TestContainers, ArchUnit, Pact contract testing |
| **DevEx** | Makefile, docker-compose, pre-commit hooks, OpenAPI docs |

## Quick Start

### Create a New Service

1. Go to [Backstage Software Catalog](https://backstage.yourcompany.com/create)
2. Select "Java Service (Golden Path)"
3. Fill in the form (see [Decision Guide](./docs/DECISIONS.md) for help)
4. Click "Create"
5. Clone your new repository and start building!

### What You'll Get

```
your-service/
├── src/
│   ├── main/java/          # Application code
│   └── test/java/          # Tests (unit, integration, contract)
├── k8s/                    # Kubernetes manifests
├── .github/                # CI/CD workflows
├── docs/                   # Documentation
│   ├── GETTING_STARTED.md  # First steps
│   ├── PATTERNS.md         # Enterprise patterns
│   ├── EXTENDING.md        # Customization guide
│   └── adr/                # Architecture decisions
├── docker-compose.yaml     # Local development
├── Makefile                # Developer commands
└── README.md               # Service documentation
```

## Documentation

| Document | Description |
|----------|-------------|
| [Decision Guide](./docs/DECISIONS.md) | How to choose template options |
| [Golden Path Overview](./docs/index.md) | What and why |
| [Getting Started](./skeleton/docs/GETTING_STARTED.md) | First steps after generation |
| [Patterns Guide](./skeleton/docs/PATTERNS.md) | Enterprise patterns with examples |
| [Extending Guide](./skeleton/docs/EXTENDING.md) | How to customize |

## Template Options

### Runtime
- **Java Version**: 21 (recommended) or 17
- **Spring Boot**: 3.4 (latest) or 3.3

### Data Layer
- **Database**: None, Aurora PostgreSQL, Aurora MySQL, DynamoDB
- **Cache**: None, ElastiCache Redis
- **Messaging**: None, SQS, SNS+SQS

### AI Integration
- **None**: No AI features
- **Spring AI**: Spring-native abstraction (OpenAI, Anthropic, Bedrock)
- **LangChain4j**: Full AI toolkit (RAG, agents, tools)
- **Direct SDKs**: OpenAI, Anthropic, or Bedrock directly

### Developer Experience
- **Feature Flags**: Togglz for gradual rollouts
- **TestContainers**: Container-based integration tests
- **ArchUnit**: Architecture validation in CI

## Enterprise Patterns

Every generated service includes these patterns (ready to use):

| Pattern | Purpose | Usage |
|---------|---------|-------|
| Idempotency | Safe retries | `@IdempotencyKey` annotation |
| Audit Logging | Compliance | `@AuditLog` annotation |
| Distributed Locking | Prevent races | `@DistributedLock` or service |
| Graceful Degradation | Handle failures | `FallbackService` |
| API Deprecation | Sunset old APIs | `@ApiDeprecation` annotation |
| Multi-tenancy | SaaS isolation | `TenantContext` |
| Transactional Outbox | Reliable events | `OutboxService` |
| Request Signing | Service auth | `RequestSigningService` |

See [Patterns Guide](./skeleton/docs/PATTERNS.md) for detailed examples.

## Architecture Decisions

We've made opinionated choices. Each is documented with rationale:

- [ADR-0001: Spring Boot 3 with Java 21](./skeleton/docs/adr/0001-use-spring-boot-3.md)
- [ADR-0002: Datadog for Observability](./skeleton/docs/adr/0002-datadog-observability.md)
- [ADR-0003: AI Integration Options](./skeleton/docs/adr/0003-ai-integration-options.md)
- [ADR-0004: Enterprise Patterns](./skeleton/docs/adr/0004-enterprise-patterns.md)

## Support

- **Slack**: #platform-help
- **Office Hours**: Thursdays 2-3pm
- **Issues**: Open in this repository

## Contributing

This template evolves based on team feedback and production learnings.

### Suggesting Changes

1. Open an issue describing the problem or enhancement
2. Discuss with platform team
3. Submit PR with changes
4. Include ADR for significant changes

### What Makes a Good Addition?

- Solves a common problem
- Has been proven in production
- Doesn't add unnecessary complexity
- Has clear documentation
- Maintains backward compatibility

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12 | Initial release |

---

🤘 Platform Team
