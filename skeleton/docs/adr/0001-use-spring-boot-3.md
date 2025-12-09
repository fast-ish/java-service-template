# ADR-0001: Use Spring Boot 3.x with Java 21

## Status

Accepted

## Date

${{ '$now' | date('YYYY-MM-DD') }}

## Context

We need to select a framework and runtime for building microservices. The choice will affect developer productivity, performance, maintainability, and hiring.

Our requirements include:
- Enterprise-grade reliability
- Strong observability support (metrics, tracing, logging)
- Wide ecosystem of integrations
- Active community and long-term support
- Good performance characteristics
- Developer familiarity

## Decision Drivers

- Team familiarity with Java ecosystem
- Enterprise support requirements
- Integration with existing infrastructure (Datadog, AWS, Kubernetes)
- Long-term support and security patches
- Performance and resource efficiency

## Considered Options

### Option 1: Spring Boot 3.x with Java 21

Latest LTS version of Spring Boot with the latest Java LTS.

**Pros:**
- Virtual threads (Project Loom) for improved concurrency
- Native compilation option via GraalVM
- Excellent Micrometer/Datadog integration
- Largest Java ecosystem
- Strong enterprise adoption

**Cons:**
- Higher memory footprint than some alternatives
- Slower cold start than native frameworks

### Option 2: Quarkus with Java 21

Cloud-native Java framework optimized for containers.

**Pros:**
- Faster startup and lower memory
- Native compilation first-class citizen
- Good Kubernetes integration

**Cons:**
- Smaller ecosystem
- Less team familiarity
- Some Spring libraries not compatible

### Option 3: Micronaut with Java 21

Compile-time DI framework.

**Pros:**
- Fast startup
- Low memory
- Compile-time DI reduces runtime overhead

**Cons:**
- Smallest ecosystem of the three
- Fewer integrations
- Learning curve for team

## Decision

We will use **Spring Boot 3.4 with Java 21** because:

1. **Team productivity**: Team is already proficient with Spring ecosystem
2. **Ecosystem**: Widest range of libraries and integrations
3. **Virtual threads**: Java 21's virtual threads address the concurrency concerns
4. **Enterprise support**: VMware Tanzu provides commercial support if needed
5. **Observability**: First-class Micrometer and Datadog integration

## Consequences

### Positive

- Developers can be productive immediately
- Wide range of problems already solved by Spring ecosystem
- Strong integration with Datadog APM
- Access to Spring Security, Spring Data, Spring Cloud

### Negative

- Higher base memory usage (~200MB vs ~50MB for native)
- Slower cold starts (~2-3s vs <1s for native)
- Must stay current with Spring Boot upgrades

### Neutral

- Need to evaluate native compilation for specific use cases
- May consider Quarkus/Micronaut for edge services with extreme latency requirements

## Implementation Notes

- Use Spring Boot 3.4.1 or latest patch version
- Enable virtual threads: `spring.threads.virtual.enabled=true`
- Configure appropriate JVM heap sizes for container environments
- Use tiered compilation for faster startup: `-XX:TieredStopAtLevel=1`

## References

- [Spring Boot 3.x Release Notes](https://spring.io/blog/category/releases)
- [Java 21 LTS Features](https://openjdk.org/projects/jdk/21/)
- [Spring Boot with Virtual Threads](https://spring.io/blog/2023/09/09/all-together-now-spring-boot-3-2-graalvm-native-images-java-21-and-virtual)
