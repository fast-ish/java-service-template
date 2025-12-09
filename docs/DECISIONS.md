# Decision Guide

This guide helps you choose the right options when creating your service. Each section explains **what**, **when**, and **why**.

## Quick Decision Matrix

| Building... | Java | Database | Cache | Messaging | AI |
|------------|------|----------|-------|-----------|-----|
| Simple API | 21 | None | None | None | None |
| CRUD Service | 21 | PostgreSQL | None | None | None |
| High-traffic API | 21 | PostgreSQL | Redis | None | None |
| Event-driven Service | 21 | PostgreSQL | Redis | SQS | None |
| AI-powered Feature | 21 | PostgreSQL | Redis | None | LangChain4j |
| Chatbot/Agent | 21 | None | None | None | LangChain4j |

---

## Java Version

### Options
- **Java 21 LTS** (Recommended)
- **Java 17 LTS**

### Decision

```
Do you have a specific library that requires Java 17?
├── Yes → Java 17
└── No → Java 21 ✓
```

### Why Java 21?
- **Virtual Threads**: Better concurrency without reactive complexity
- **Pattern Matching**: Cleaner code with switch expressions
- **Record Patterns**: Improved data handling
- **Long-term Support**: Supported until 2031

### When Java 17?
Only if you have a critical dependency that hasn't been updated for Java 21 (rare in late 2024+).

---

## Database

### Options
- **None** - Stateless service
- **Aurora PostgreSQL** (Recommended for relational)
- **Aurora MySQL** - MySQL compatibility required
- **DynamoDB** - NoSQL, high scale

### Decision

```
Does your service need to persist data?
├── No → None ✓
└── Yes
    └── What's your data model?
        ├── Relational (joins, transactions, complex queries) → PostgreSQL ✓
        ├── Key-value or document (simple access patterns) → DynamoDB
        └── Must be MySQL compatible → Aurora MySQL
```

### PostgreSQL vs DynamoDB

| Factor | PostgreSQL | DynamoDB |
|--------|------------|----------|
| Data model | Relational, complex joins | Key-value, document |
| Transactions | Full ACID | Limited (single table) |
| Query flexibility | Any query | Primary key + indexes only |
| Scaling | Vertical + read replicas | Automatic horizontal |
| Cost model | Instance-based | Pay per request |
| Best for | Complex domains, reporting | High-volume, simple access |

### Why Not MySQL?
PostgreSQL has better:
- JSON support (JSONB)
- Full-text search
- Array types
- Window functions
- Tooling (pg_dump, pg_restore)

Use MySQL only if migrating an existing MySQL database.

---

## Cache

### Options
- **None** - No caching needed
- **ElastiCache Redis** - Distributed caching

### Decision

```
Will your service benefit from caching?
├── Read-heavy with stable data → Redis ✓
├── Real-time data (always fresh) → None
├── Simple service, low traffic → None
└── Session storage needed → Redis ✓
```

### When to Add Redis

| Use Case | Add Redis? |
|----------|------------|
| Product catalog (updates hourly) | Yes |
| User profile (updates rarely) | Yes |
| Real-time inventory | No (stale data = overselling) |
| Session storage | Yes |
| Rate limiting | Yes |
| Distributed locks | Yes |
| Simple CRUD, low traffic | No (premature optimization) |

### Cache Invalidation Strategy
This template includes cache patterns. Choose based on data:
- **TTL-based**: Set expiry, accept staleness
- **Write-through**: Update cache on write
- **Event-driven**: Invalidate on domain events

---

## Messaging

### Options
- **None** - Synchronous only
- **SQS** - Queue consumer
- **SNS + SQS** - Pub/Sub with queue

### Decision

```
Does your service need async processing?
├── No → None ✓
└── Yes
    └── How many consumers per message?
        ├── One (work queue) → SQS ✓
        └── Multiple (fan-out) → SNS + SQS ✓
```

### SQS Patterns

| Pattern | Description | Example |
|---------|-------------|---------|
| Work Queue | One consumer processes each message | Order processing |
| Fan-out | Multiple consumers, same message | Order → Email + Analytics + Inventory |
| Dead Letter | Failed messages for investigation | Retry exhausted |

### When to Use Messaging

| Scenario | Sync or Async? |
|----------|----------------|
| User expects immediate response | Sync |
| Long-running operation | Async (return job ID) |
| Eventual consistency OK | Async |
| Cross-service communication | Async (loose coupling) |
| Webhook processing | Async (queue for reliability) |

---

## AI Integration

### Options
- **None** - No AI features
- **Spring AI** (Recommended for Spring ecosystem)
- **LangChain4j** (Recommended for advanced AI)
- **OpenAI SDK** - Direct OpenAI access
- **Anthropic SDK** - Direct Claude access
- **AWS Bedrock** - Multi-model, AWS-native

### Decision

```
Does your service need AI capabilities?
├── No → None ✓
└── Yes
    └── What features do you need?
        ├── Basic chat/completion only
        │   ├── Want Spring-native? → Spring AI ✓
        │   └── Prefer lightweight? → Direct SDK
        ├── RAG (retrieval-augmented generation) → LangChain4j ✓
        ├── AI Agents with tools → LangChain4j ✓
        ├── Multiple models (compare/fallback) → LangChain4j or Bedrock
        └── AWS-only, no API keys → Bedrock ✓
```

### AI Framework Comparison

| Feature | Spring AI | LangChain4j | Direct SDKs |
|---------|-----------|-------------|-------------|
| Chat completion | ✓ | ✓ | ✓ |
| Embeddings | ✓ | ✓ | ✓ |
| RAG | Basic | Advanced | Manual |
| Agents/Tools | Limited | Full support | Manual |
| Memory | Basic | Full support | Manual |
| Vector stores | Some | Many | N/A |
| Spring integration | Native | Via starters | Manual |
| Learning curve | Low | Medium | Low |

### Provider Comparison

| Provider | Best For | Considerations |
|----------|----------|----------------|
| OpenAI | General purpose, GPT-4o | API key management |
| Anthropic | Claude, longer context | API key management |
| Bedrock | AWS shops, multi-model | AWS IAM, latency |

---

## Observability (SRE)

### Distributed Tracing
**Always enabled.** Required for debugging distributed systems.

### Prometheus Metrics
**Always enabled.** Required for alerting and dashboards.

### SLO Targets

| Use Case | Availability | Latency (P99) |
|----------|--------------|---------------|
| Customer-facing API | 99.9% | 200ms |
| Internal API | 99.5% | 500ms |
| Batch processing | 99.0% | 5s |
| Best-effort feature | 99.0% | 1s |

```
What's your service's criticality?
├── Customer-facing, revenue-impacting → 99.9%, 200ms
├── Internal, other services depend on it → 99.5%, 500ms
├── Background processing → 99.0%, 5s
└── Nice-to-have feature → 99.0%, 1s
```

---

## Developer Experience

### Feature Flags
```
Will you do gradual rollouts or A/B testing?
├── Yes → Enable ✓
└── No → Skip (can add later)
```

### TestContainers
```
Do you have a database or Redis?
├── Yes → Enable ✓ (realistic integration tests)
└── No → Optional
```

### ArchUnit
```
Do you want automated architecture enforcement?
├── Yes → Enable ✓ (catches violations in CI)
└── No → Skip (can add later)
```

---

## Recommended Configurations

### Simple API (Hello World+)
```yaml
javaVersion: 21
database: none
cache: none
messaging: none
aiClient: none
featureFlags: false
```

### Standard CRUD Service
```yaml
javaVersion: 21
database: aurora-postgresql
cache: none
messaging: none
aiClient: none
featureFlags: false
testContainers: true
archUnit: true
```

### High-Traffic API
```yaml
javaVersion: 21
database: aurora-postgresql
cache: elasticache-redis
messaging: none
aiClient: none
featureFlags: true
testContainers: true
archUnit: true
```

### Event-Driven Microservice
```yaml
javaVersion: 21
database: aurora-postgresql
cache: elasticache-redis
messaging: sns-sqs
aiClient: none
featureFlags: true
testContainers: true
archUnit: true
```

### AI-Powered Service
```yaml
javaVersion: 21
database: aurora-postgresql
cache: elasticache-redis
messaging: sqs
aiClient: langchain4j
aiProvider: anthropic
featureFlags: true
testContainers: true
archUnit: true
```

---

## Still Unsure?

### Default to Simple
When in doubt, choose the simpler option. You can always add complexity later, but removing it is harder.

### Ask the Platform Team
Post in #platform-help with your use case. We're happy to advise.

### Start, Then Iterate
This template supports incremental enhancement. Start with what you need today, add more as requirements evolve.
