# ADR-0002: Use Datadog for Observability

## Status

Accepted

## Date

${{ '$now' | date('YYYY-MM-DD') }}

## Context

We need a comprehensive observability platform that provides:
- Application Performance Monitoring (APM)
- Distributed tracing
- Metrics collection and visualization
- Log aggregation and analysis
- Alerting and SLO management

The platform must integrate well with our Kubernetes infrastructure and support Google SRE practices for reliability management.

## Decision Drivers

- Unified platform for metrics, traces, and logs
- Kubernetes-native integration
- SLO/SLI management capabilities
- Team familiarity
- Cost at scale
- Integration with existing tools

## Considered Options

### Option 1: Datadog

Commercial observability platform.

**Pros:**
- All-in-one platform (APM, logs, metrics, synthetics)
- Excellent Kubernetes integration
- Built-in SLO management
- Strong Java/Spring Boot auto-instrumentation
- Real-time dashboards and alerting

**Cons:**
- Higher cost at scale
- Vendor lock-in
- Data egress considerations

### Option 2: Grafana Stack (Prometheus + Loki + Tempo)

Open-source observability stack.

**Pros:**
- No licensing costs
- Full control over data
- Large community
- Flexible and customizable

**Cons:**
- Operational overhead
- Need to manage multiple systems
- Integration complexity
- No built-in SLO management (need separate tooling)

### Option 3: AWS Native (CloudWatch + X-Ray)

AWS-native observability.

**Pros:**
- Native AWS integration
- No additional vendors
- Pay-per-use pricing

**Cons:**
- Less powerful APM than dedicated solutions
- Limited cross-service correlation
- AWS-specific (reduces portability)

## Decision

We will use **Datadog** because:

1. **Unified platform**: Single pane of glass for all observability needs
2. **SLO support**: Native SLO management aligns with Google SRE practices
3. **Java integration**: Excellent dd-java-agent with automatic instrumentation
4. **Kubernetes**: Deep EKS integration with autodiscovery
5. **Time to value**: Faster setup than self-managed alternatives

## Consequences

### Positive

- Rapid onboarding with auto-instrumentation
- Correlated metrics, traces, and logs out of the box
- Built-in SLO tracking and error budgets
- Reduced operational overhead vs self-managed

### Negative

- Recurring licensing costs
- Need to manage Datadog API keys securely
- Some advanced features require higher tiers
- Data residency considerations for certain regulations

### Neutral

- Must maintain dd-java-agent version updates
- Teams need Datadog training

## Implementation Notes

- Use Unified Service Tagging: `env`, `service`, `version`
- Deploy Datadog Agent as DaemonSet in Kubernetes
- Use admission controller for automatic instrumentation
- Configure log parsing for JSON structured logs
- Set up SLOs based on latency and error rate

Key configurations:
```yaml
# Kubernetes labels for unified service tagging
labels:
  tags.datadoghq.com/env: production
  tags.datadoghq.com/service: my-service
  tags.datadoghq.com/version: 1.0.0
```

## References

- [Datadog Java Tracing](https://docs.datadoghq.com/tracing/setup_overview/setup/java/)
- [Datadog Kubernetes Integration](https://docs.datadoghq.com/containers/kubernetes/)
- [Datadog SLO Management](https://docs.datadoghq.com/service_management/service_level_objectives/)
- [Google SRE Workbook](https://sre.google/workbook/table-of-contents/)
