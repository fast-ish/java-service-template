# Java Service Golden Path Template

Backstage software template for generating production-ready Spring Boot 3.4.1 services with OpenTelemetry observability (Grafana stack).

## Structure

```
/template.yaml          # Backstage scaffolder definition (all parameters here)
/skeleton/              # Generated service template (Jinja2 templated)
/docs/                  # Template-level documentation
```

## Key Files

- `template.yaml` - Template parameters and steps (scaffolder.backstage.io/v1beta3)
- `skeleton/pom.xml` - Maven build with conditional dependencies
- `skeleton/src/main/resources/application.yaml` - All Spring config
- `skeleton/docs/PATTERNS.md` - Enterprise pattern documentation with examples

## Template Syntax

Uses Jinja2 via Backstage:
- Variables: `${{values.name}}`, `${{values.owner}}`
- Conditionals: `{%- if values.database != "none" %}...{%- endif %}`
- Filters: `${{values.name | lower}}`

## Testing Template Changes

```bash
cd skeleton
./mvnw clean verify                    # Build and test
./mvnw checkstyle:check spotbugs:check # Code quality
docker build -t test .                 # Container build
```

## Template Options

| Parameter | Values |
|-----------|--------|
| database | none, aurora-postgresql, aurora-mysql, dynamodb |
| cache | none, elasticache-redis |
| messaging | none, sqs, sns-sqs |
| aiClient | none, spring-ai, langchain4j, openai, anthropic, bedrock |

## Conventions

- Google Java Style (enforced by Checkstyle)
- All REST endpoints return `ApiResponse<T>` wrapper
- Enterprise patterns use annotations (`@Idempotent`, `@AuditLog`, `@DistributedLock`)
- Metrics via Micrometer with `@Timed` annotation
- Structured JSON logging for Grafana Loki

## Version Pinning

Keep these current:
- Spring Boot: 3.4.1
- opentelemetry-javaagent: latest
- AWS SDK: 2.29.45
- LangChain4j: 0.36.2

## Don't

- Add backwards-compatibility shims - just update the template
- Create new files when editing existing ones works
- Add features without updating docs/PATTERNS.md
- Use placeholder versions - verify latest on Maven Central
