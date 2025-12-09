# Architecture Overview

## Service Architecture

```mermaid
flowchart TB
    subgraph External
        Client[Client Apps]
        DD[Datadog]
    end

    subgraph Kubernetes
        subgraph Service["${{values.name}}"]
            API[REST API :8080]
            Mgmt[Management :8081]
        end

        {%- if values.database != "none" %}
        subgraph Data
            DB[(Database)]
        end
        {%- endif %}

        {%- if values.cache == "elasticache-redis" %}
        subgraph Cache
            Redis[(Redis)]
        end
        {%- endif %}

        {%- if values.messaging != "none" %}
        subgraph Messaging
            SQS[SQS Queue]
            {%- if values.messaging == "sns-sqs" %}
            SNS[SNS Topic]
            {%- endif %}
        end
        {%- endif %}
    end

    Client --> API
    Mgmt --> DD

    {%- if values.database != "none" %}
    API --> DB
    {%- endif %}

    {%- if values.cache == "elasticache-redis" %}
    API --> Redis
    {%- endif %}

    {%- if values.messaging == "sns-sqs" %}
    API --> SNS
    SNS --> SQS
    {%- elif values.messaging == "sqs" %}
    SQS --> API
    {%- endif %}
```

## Request Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant F as Filter Chain
    participant Ctrl as Controller
    participant Svc as Service
    {%- if values.database != "none" %}
    participant DB as Database
    {%- endif %}
    participant DD as Datadog

    C->>F: HTTP Request
    Note over F: Correlation ID
    Note over F: Tenant Context
    Note over F: Security Headers

    F->>Ctrl: Validated Request
    Note over Ctrl: @Timed
    Note over Ctrl: @AuditLog

    Ctrl->>Svc: Business Logic
    {%- if values.database != "none" %}
    Svc->>DB: Query/Persist
    DB-->>Svc: Result
    {%- endif %}

    Svc-->>Ctrl: Response
    Ctrl-->>F: ApiResponse<T>
    F-->>C: JSON Response

    F--)DD: Trace + Metrics
```

## Component Diagram

```mermaid
graph LR
    subgraph Controller Layer
        AC[ApiController]
        EC[ExampleController]
        {%- if values.aiClient != "none" %}
        AIC[AiController]
        {%- endif %}
    end

    subgraph Service Layer
        ES[ExampleService]
        {%- if values.aiClient != "none" %}
        AIS[AiService]
        {%- endif %}
        OS[OutboxService]
        AS[AuditService]
    end

    subgraph Infrastructure
        LS[LockService]
        IS[IdempotencyService]
        FS[FallbackService]
    end

    subgraph Config
        OC[OpenApiConfig]
        SC[SecurityConfig]
        LC[LoggingConfig]
        AC2[AsyncConfig]
    end

    EC --> ES
    {%- if values.aiClient != "none" %}
    AIC --> AIS
    {%- endif %}
    ES --> OS
    ES --> AS
    ES --> LS
```

## Resilience Patterns

```mermaid
flowchart LR
    subgraph Request
        R[Incoming Request]
    end

    subgraph Resilience
        CB{Circuit Breaker}
        RT[Retry]
        TL[Time Limiter]
        BH[Bulkhead]
        FB[Fallback]
    end

    subgraph Outcomes
        S[Success]
        F[Fallback Response]
        E[Error]
    end

    R --> CB
    CB -->|Closed| RT
    CB -->|Open| FB
    RT -->|Success| S
    RT -->|Failure| TL
    TL -->|Timeout| FB
    TL -->|Success| S
    FB --> F
    BH -.->|Limit Concurrency| RT
```

## Data Flow (Event-Driven)

{%- if values.messaging != "none" %}
```mermaid
flowchart LR
    subgraph Producer Service
        API[API Handler]
        TX[(Transaction)]
        OB[Outbox Table]
        OP[Outbox Processor]
    end

    subgraph Message Broker
        {%- if values.messaging == "sns-sqs" %}
        SNS[SNS Topic]
        {%- endif %}
        SQS[SQS Queue]
        DLQ[Dead Letter Queue]
    end

    subgraph Consumer Service
        LC[Listener]
        CS[Consumer Service]
    end

    API -->|1. Business Logic| TX
    TX -->|2. Write Event| OB
    OP -->|3. Poll & Publish| {%- if values.messaging == "sns-sqs" %}SNS{%- else %}SQS{%- endif %}
    {%- if values.messaging == "sns-sqs" %}
    SNS -->|4. Fan-out| SQS
    {%- endif %}
    SQS -->|5. Receive| LC
    LC -->|6. Process| CS
    SQS -->|Failed| DLQ
```
{%- else %}
```mermaid
flowchart LR
    A[Service A] -->|REST| B[Service B]
    B -->|REST| C[Service C]

    Note[Enable messaging for event-driven patterns]
```
{%- endif %}

## Deployment Architecture

```mermaid
flowchart TB
    subgraph GitHub
        Repo[Repository]
        Actions[GitHub Actions]
    end

    subgraph Container Registry
        ECR[ECR]
    end

    subgraph Kubernetes Cluster
        subgraph Namespace
            Deploy[Deployment]
            SVC[Service]
            HPA[HPA]
            PDB[PDB]
        end

        subgraph Observability
            DDAgent[Datadog Agent]
        end
    end

    subgraph AWS
        {%- if values.database == "aurora-postgresql" or values.database == "aurora-mysql" %}
        Aurora[(Aurora)]
        {%- endif %}
        {%- if values.database == "dynamodb" %}
        DDB[(DynamoDB)]
        {%- endif %}
        {%- if values.cache == "elasticache-redis" %}
        ElastiCache[(ElastiCache)]
        {%- endif %}
        SM[Secrets Manager]
    end

    Repo -->|Push| Actions
    Actions -->|Build & Push| ECR
    Actions -->|Deploy| Deploy
    ECR --> Deploy
    SM --> Deploy
    Deploy --> SVC
    HPA --> Deploy
    PDB --> Deploy
    DDAgent --> Deploy

    {%- if values.database == "aurora-postgresql" or values.database == "aurora-mysql" %}
    Deploy --> Aurora
    {%- endif %}
    {%- if values.database == "dynamodb" %}
    Deploy --> DDB
    {%- endif %}
    {%- if values.cache == "elasticache-redis" %}
    Deploy --> ElastiCache
    {%- endif %}
```

## Security Model

```mermaid
flowchart TB
    subgraph External
        Client[Client]
    end

    subgraph Edge
        LB[Load Balancer]
        WAF[WAF]
    end

    subgraph Service
        Headers[Security Headers Filter]
        Auth[Authentication]
        CORS[CORS Filter]
        Sign[Request Signing]
        Tenant[Tenant Context]
        API[API Layer]
    end

    subgraph Internal Services
        S2S[Service-to-Service]
    end

    Client -->|HTTPS| WAF
    WAF --> LB
    LB --> Headers
    Headers --> CORS
    CORS --> Auth
    Auth --> Tenant
    Tenant --> API

    S2S <-->|Signed Requests| Sign
    Sign --> API
```
