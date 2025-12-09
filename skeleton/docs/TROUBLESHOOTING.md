# Troubleshooting Guide

Quick solutions to common issues.

## Build Issues

### "Java version mismatch"

```
Error: Java 21 is required but Java 17 was found
```

**Solution**: Install Java 21 and set JAVA_HOME:
```bash
# macOS (with Homebrew)
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21

# Verify
java -version
```

### "Maven wrapper not found"

```
Error: ./mvnw: No such file or directory
```

**Solution**: Make the wrapper executable or regenerate:
```bash
chmod +x mvnw
# or
mvn wrapper:wrapper
```

### "Out of memory during build"

**Solution**: Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx2g"
make build
```

---

## Runtime Issues

### "Port 8080 already in use"

```
Error: Web server failed to start. Port 8080 was already in use.
```

**Solution**: Find and kill the process or use a different port:
```bash
# Find what's using the port
lsof -i :8080

# Kill it
kill -9 <PID>

# Or use a different port
SERVER_PORT=8081 make run
```

### "Connection refused to database"

```
Error: Connection to localhost:5432 refused
```

**Solution**: Start local dependencies:
```bash
make deps-up
docker-compose ps  # Verify containers are running
```

### "Health check failing"

**Solution**: Check the logs and verify dependencies:
```bash
# Check application logs
make deps-logs

# Verify health endpoint
curl -v http://localhost:8081/health

# Check readiness details
curl http://localhost:8081/health/readiness | jq
```

### "Datadog traces not appearing"

**Solution**: Verify Datadog configuration:
```bash
# Check DD_AGENT_HOST is set
echo $DD_AGENT_HOST

# For local development, either:
# 1. Disable tracing
DD_TRACE_ENABLED=false make run

# 2. Or run Datadog agent
docker run -d --name dd-agent \
  -e DD_API_KEY=your-key \
  -e DD_APM_ENABLED=true \
  -p 8126:8126 \
  gcr.io/datadoghq/agent:7
```

---

## Docker Issues

### "Docker build fails: cannot find Dockerfile"

**Solution**: Run from project root:
```bash
cd /path/to/your-service
docker build -t your-service .
```

### "Container exits immediately"

**Solution**: Check container logs:
```bash
docker logs <container-id>

# Or run interactively
docker run -it your-service:latest /bin/sh
```

### "Out of disk space"

**Solution**: Clean up Docker:
```bash
docker system prune -a
docker volume prune
```

---

## Test Issues

### "TestContainers: Docker not available"

```
Error: Could not find a valid Docker environment
```

**Solution**: Ensure Docker is running:
```bash
# macOS
open -a Docker

# Verify
docker ps
```

### "Tests fail with "connection refused""

**Solution**: Tests might be running before containers are ready. Add wait:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withStartupTimeout(Duration.ofMinutes(2));
```

### "Checkstyle violations"

**Solution**: Auto-format your code:
```bash
# Check violations
make lint

# Fix common issues (if Spotless configured)
./mvnw spotless:apply

# Or manually fix based on checkstyle output
```

---

## Kubernetes Issues

### "Pod stuck in CrashLoopBackOff"

**Solution**: Check pod logs and events:
```bash
kubectl logs deployment/your-service
kubectl describe pod <pod-name>
kubectl get events --sort-by=.lastTimestamp
```

### "Readiness probe failing"

**Solution**: Verify the probe endpoint works:
```bash
# Exec into pod
kubectl exec -it <pod-name> -- curl localhost:8081/health/readiness

# Check probe configuration
kubectl get deployment your-service -o yaml | grep -A10 readinessProbe
```

### "Secret not found"

```
Error: secret "your-service-secrets" not found
```

**Solution**: Create the secret or ExternalSecret:
```bash
# Check if ExternalSecret exists
kubectl get externalsecret

# Check ExternalSecret status
kubectl describe externalsecret your-service

# Verify AWS Secrets Manager secret exists
aws secretsmanager describe-secret --secret-id your-team/your-service
```

---

## AI Integration Issues

### "OpenAI: 401 Unauthorized"

**Solution**: Check API key:
```bash
# Verify key is set
echo $OPENAI_API_KEY

# Test key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

### "Anthropic: Rate limited"

**Solution**: Implement backoff or check usage:
```bash
# Check your usage at https://console.anthropic.com/
# Consider implementing rate limiting in your service
```

### "Bedrock: Access denied"

**Solution**: Check IAM permissions:
```bash
# Verify credentials
aws sts get-caller-identity

# Test Bedrock access
aws bedrock list-foundation-models --region us-east-1
```

---

## Still Stuck?

1. **Check the logs**: `make deps-logs` or `kubectl logs`
2. **Search existing issues**: Check the template repository
3. **Ask in Slack**: #platform-help
4. **Office hours**: Thursdays 2-3pm

When asking for help, include:
- Error message (full stack trace)
- Steps to reproduce
- What you've already tried
- Environment (local/dev/prod, OS, Java version)
