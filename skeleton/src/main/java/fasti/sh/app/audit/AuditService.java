package fasti.sh.app.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fasti.sh.app.config.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for recording audit logs.
 *
 * For production, integrate with:
 * - Database (PostgreSQL/DynamoDB)
 * - Log aggregation (Datadog, Splunk)
 * - Event streaming (Kafka, SQS)
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    private static final String REDACTED = "[REDACTED]";

    private final ObjectMapper objectMapper;
    private final Counter auditEventsCounter;

    // In-memory queue for async processing - replace with proper persistence
    private final ConcurrentLinkedQueue<AuditEvent> eventQueue = new ConcurrentLinkedQueue<>();

    public AuditService(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.auditEventsCounter = Counter.builder("audit.events")
            .description("Number of audit events recorded")
            .register(meterRegistry);
    }

    /**
     * Record an audit event.
     */
    @Async
    public void record(AuditEventBuilder builder) {
        AuditEvent event = builder
            .timestamp(Instant.now())
            .correlationId(MDC.get("correlationId"))
            .tenantId(TenantContext.getTenantId())
            .build();

        // Log to dedicated audit logger
        auditLogger.info("{}", serializeEvent(event));

        // Queue for persistence
        eventQueue.offer(event);

        auditEventsCounter.increment();
        log.debug("Recorded audit event: {} on {}/{}",
            event.action(), event.resourceType(), event.resourceId());
    }

    /**
     * Redact sensitive fields from a JSON object.
     */
    public String redactSensitiveFields(Object body, Set<String> fieldsToRedact) {
        if (body == null) {
            return null;
        }

        try {
            JsonNode node = objectMapper.valueToTree(body);
            redactNode(node, fieldsToRedact);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Failed to redact sensitive fields", e);
            return "[REDACTION_FAILED]";
        }
    }

    private void redactNode(JsonNode node, Set<String> fieldsToRedact) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (fieldsToRedact.stream().anyMatch(f ->
                    fieldName.toLowerCase().contains(f.toLowerCase()))) {
                    objectNode.put(fieldName, REDACTED);
                } else {
                    redactNode(objectNode.get(fieldName), fieldsToRedact);
                }
            });
        } else if (node.isArray()) {
            node.forEach(element -> redactNode(element, fieldsToRedact));
        }
    }

    private String serializeEvent(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize audit event", e);
            return event.toString();
        }
    }

    /**
     * Flush queued events to persistent storage.
     * Call this periodically or on shutdown.
     */
    public void flush() {
        // Implement batch persistence to database/event stream
        while (!eventQueue.isEmpty()) {
            AuditEvent event = eventQueue.poll();
            // TODO: Persist to database or send to event stream
        }
    }

    // Builder pattern for audit events
    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    public record AuditEvent(
        Instant timestamp,
        String userId,
        String tenantId,
        String action,
        String resourceType,
        String resourceId,
        String correlationId,
        String ipAddress,
        String userAgent,
        String requestMethod,
        String requestPath,
        String requestBody,
        Integer responseStatus,
        String responseBody,
        Long durationMs,
        Map<String, Object> metadata
    ) {}

    public static class AuditEventBuilder {
        private Instant timestamp;
        private String userId;
        private String tenantId;
        private String action;
        private String resourceType;
        private String resourceId;
        private String correlationId;
        private String ipAddress;
        private String userAgent;
        private String requestMethod;
        private String requestPath;
        private String requestBody;
        private Integer responseStatus;
        private String responseBody;
        private Long durationMs;
        private Map<String, Object> metadata;

        public AuditEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditEventBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public AuditEventBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public AuditEventBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditEventBuilder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public AuditEventBuilder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public AuditEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public AuditEventBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AuditEventBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public AuditEventBuilder requestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public AuditEventBuilder requestPath(String requestPath) {
            this.requestPath = requestPath;
            return this;
        }

        public AuditEventBuilder requestBody(String requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public AuditEventBuilder responseStatus(Integer responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }

        public AuditEventBuilder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public AuditEventBuilder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public AuditEventBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(
                timestamp, userId, tenantId, action, resourceType, resourceId,
                correlationId, ipAddress, userAgent, requestMethod, requestPath,
                requestBody, responseStatus, responseBody, durationMs, metadata
            );
        }
    }
}
