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

# Install curl for healthchecks and download OpenTelemetry agent
RUN apk add --no-cache curl && \
    mkdir -p /opt/otel && \
    curl -Lo /opt/otel/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Create non-root user
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R app:app /app /opt/otel

USER app

# JVM options for containers with OpenTelemetry agent
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# OpenTelemetry Configuration
ENV OTEL_SERVICE_NAME="${{values.name}}"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://grafana-alloy:4318"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="otlp"
ENV OTEL_RESOURCE_ATTRIBUTES="service.namespace=team-${{values.owner}}"

# Application port and management port
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/opt/otel/opentelemetry-javaagent.jar -jar app.jar"]
