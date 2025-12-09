# Changelog

All notable changes to this service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial service creation from Golden Path template

### Changed

### Deprecated

### Removed

### Fixed

### Security

---

## [0.1.0] - ${{ '$now' | date('YYYY-MM-DD') }}

### Added
- Initial project structure
- Health check endpoints (liveness, readiness, startup)
- Prometheus metrics endpoint
- Structured JSON logging for Datadog
- OpenAPI documentation (Swagger UI)
- Example CRUD endpoints
- Exception handling framework
- Docker containerization
- Kubernetes manifests
- CI/CD pipeline (GitHub Actions)
- Code quality tools (Checkstyle, SpotBugs, PMD)
- Security scanning (OWASP, Trivy)
{%- if values.database != "none" %}
- Database integration ({%- if values.database == "aurora-postgresql" %}PostgreSQL{%- elif values.database == "aurora-mysql" %}MySQL{%- else %}DynamoDB{%- endif %})
- Flyway database migrations
{%- endif %}
{%- if values.cache == "elasticache-redis" %}
- Redis caching support
{%- endif %}
{%- if values.messaging != "none" %}
- AWS messaging ({%- if values.messaging == "sqs" %}SQS{%- else %}SNS + SQS{%- endif %})
{%- endif %}
{%- if values.aiClient != "none" %}
- AI integration ({%- if values.aiClient == "spring-ai" %}Spring AI{%- elif values.aiClient == "langchain4j" %}LangChain4j{%- elif values.aiClient == "openai" %}OpenAI SDK{%- elif values.aiClient == "anthropic" %}Anthropic SDK{%- else %}AWS Bedrock{%- endif %})
{%- endif %}

---

## Release Notes Format

When releasing, document changes like this:

```markdown
## [1.0.0] - 2024-03-15

### Added
- User authentication endpoint
- Rate limiting for public APIs

### Changed
- Improved error messages for validation failures
- Updated Spring Boot to 3.4.2

### Fixed
- Memory leak in connection pool (#123)
- Race condition in order processing (#124)

### Security
- Updated dependencies to patch CVE-XXXX-YYYY
```

---

[Unreleased]: https://github.com/${{values.owner}}/${{values.name}}/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/${{values.owner}}/${{values.name}}/releases/tag/v0.1.0
