# Java Service Golden Path

> **Golden Path**: An opinionated, well-supported path for building services that represent our best practices and standards.

This template embodies our organization's collective knowledge about building production-ready Java services. Following this path means you're building on proven patterns with full platform support.

## Why a Golden Path?

| Without Golden Path | With Golden Path |
|---------------------|------------------|
| Every team makes different choices | Consistent patterns across services |
| Reinvent observability for each service | Built-in Datadog integration |
| Security as an afterthought | Security baked in from day one |
| "Works on my machine" | Reproducible, containerized builds |
| Manual deployment processes | GitOps with ArgoCD |
| Tribal knowledge | Documented patterns |

## What You Get

When you create a service from this template, you get:

### Day 1 Ready
- ✅ Compiles and runs immediately
- ✅ Health checks configured
- ✅ Logging in correct format
- ✅ Traces flowing to Datadog
- ✅ Metrics exposed for Prometheus
- ✅ Kubernetes manifests ready
- ✅ CI/CD pipeline configured

### Production Patterns
- ✅ Structured exception handling
- ✅ Request validation
- ✅ API documentation (OpenAPI)
- ✅ Resilience (circuit breakers, retries)
- ✅ Idempotency support
- ✅ Audit logging

### Enterprise Ready
- ✅ Multi-tenancy support
- ✅ Database migrations (Flyway)
- ✅ Contract testing (Pact)
- ✅ Security scanning (SAST, DAST)
- ✅ Dependency vulnerability scanning

## Guiding Principles

### 1. Convention Over Configuration
We make opinionated choices so you don't have to. These aren't arbitrary - they're based on production experience and platform requirements.

### 2. Secure by Default
Security features are enabled, not opt-in. You can relax them if needed, but the default is secure.

### 3. Observable from the Start
Every service needs monitoring. It's not optional, so we don't make it optional.

### 4. Ready for Scale
Patterns that work at 10 RPS work at 10,000 RPS. We build for where we're going.

### 5. Team Autonomy with Guardrails
Teams can extend and customize, but within patterns that keep the platform healthy.

## Documentation Structure

| Document | Purpose |
|----------|---------|
| [Decision Guide](./DECISIONS.md) | How to choose template options |
| [Getting Started](../skeleton/docs/GETTING_STARTED.md) | First steps after generation |
| [Patterns Guide](../skeleton/docs/PATTERNS.md) | Enterprise patterns with examples |
| [Extending Guide](../skeleton/docs/EXTENDING.md) | How to customize your service |
| [ADRs](../skeleton/docs/adr/) | Why we made specific choices |

## Quick Links

- **Create a Service**: [Backstage Software Catalog](https://backstage.yourcompany.com/create)
- **Platform Support**: #platform-help on Slack
- **Template Source**: This repository
- **Request Features**: Open an issue in this repo
