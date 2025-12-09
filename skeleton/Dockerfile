# Build stage
FROM eclipse-temurin:${{values.javaVersion | default("21", true)}}-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:${{values.javaVersion | default("21", true)}}-jre-alpine
WORKDIR /app

# Install curl for healthchecks and download Datadog agent
RUN apk add --no-cache curl && \
    mkdir -p /opt/datadog && \
    curl -Lo /opt/datadog/dd-java-agent.jar https://dtdg.co/latest-java-tracer

# Create non-root user
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R app:app /app /opt/datadog

USER app

# JVM options for containers with Datadog agent
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Datadog APM Configuration
ENV DD_AGENT_HOST="datadog-agent"
ENV DD_SERVICE="${{values.name}}"
ENV DD_ENV="${ENVIRONMENT:-development}"
ENV DD_VERSION="${APP_VERSION:-0.1.0}"
ENV DD_TRACE_ENABLED="true"
ENV DD_LOGS_INJECTION="true"
ENV DD_PROFILING_ENABLED="true"
ENV DD_APPSEC_ENABLED="true"
ENV DD_RUNTIME_METRICS_ENABLED="true"
ENV DD_TRACE_SAMPLE_RATE="1.0"
ENV DD_TRACE_ANALYTICS_ENABLED="true"
ENV DD_JMXFETCH_ENABLED="true"

# Application port and management port
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/opt/datadog/dd-java-agent.jar -jar app.jar"]
