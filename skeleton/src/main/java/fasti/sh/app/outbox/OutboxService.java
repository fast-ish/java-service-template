package fasti.sh.app.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Service for managing the transactional outbox.
 *
 * For production, replace the in-memory queue with:
 * - Database table (PostgreSQL/MySQL) with JPA repository
 * - Debezium for CDC-based outbox processing
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // In-memory queue - replace with database for production
    private final Queue<OutboxEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final Map<String, Consumer<OutboxEvent>> eventHandlers = new ConcurrentHashMap<>();

    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;

    public OutboxService(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        this.eventsPublishedCounter = Counter.builder("outbox.events.published")
            .description("Number of outbox events successfully published")
            .register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("outbox.events.failed")
            .description("Number of outbox events that failed to publish")
            .register(meterRegistry);

        Gauge.builder("outbox.queue.size", pendingEvents, Queue::size)
            .description("Number of pending outbox events")
            .register(meterRegistry);
    }

    /**
     * Add an event to the outbox.
     * In production, this should be done in the same transaction as the business operation.
     */
    public void publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.create(aggregateType, aggregateId, eventType, payloadJson);
            pendingEvents.offer(event);
            log.debug("Added event to outbox: {} {} {}", aggregateType, aggregateId, eventType);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to publish outbox event", e);
        }
    }

    /**
     * Register a handler for a specific event type.
     */
    public void registerHandler(String eventType, Consumer<OutboxEvent> handler) {
        eventHandlers.put(eventType, handler);
        log.info("Registered outbox handler for event type: {}", eventType);
    }

    /**
     * Process pending outbox events.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelayString = "${outbox.poll-interval:5000}")
    public void processPendingEvents() {
        int processed = 0;
        OutboxEvent event;

        while (processed < BATCH_SIZE && (event = pendingEvents.poll()) != null) {
            try {
                processEvent(event);
                eventsPublishedCounter.increment();
                processed++;
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.id(), e);
                handleFailedEvent(event, e.getMessage());
            }
        }

        if (processed > 0) {
            log.debug("Processed {} outbox events", processed);
        }
    }

    private void processEvent(OutboxEvent event) {
        Consumer<OutboxEvent> handler = eventHandlers.get(event.eventType());

        if (handler != null) {
            handler.accept(event);
        } else {
            // Default: log the event (replace with actual publishing to SQS/SNS/Kafka)
            log.info("Publishing outbox event: type={}, aggregateType={}, aggregateId={}",
                event.eventType(), event.aggregateType(), event.aggregateId());

            // TODO: Replace with actual message broker publishing
            // sqsClient.sendMessage(...)
            // kafkaTemplate.send(...)
            // snsClient.publish(...)
        }
    }

    private void handleFailedEvent(OutboxEvent event, String error) {
        OutboxEvent failed = event.markFailed(error);
        eventsFailedCounter.increment();

        if (failed.shouldRetry(MAX_RETRIES)) {
            // Re-queue for retry with exponential backoff
            pendingEvents.offer(failed);
            log.warn("Outbox event {} failed, will retry (attempt {})",
                event.id(), failed.retryCount());
        } else {
            // Move to dead letter queue / alert
            log.error("Outbox event {} exceeded max retries, moving to DLQ", event.id());
            // TODO: Persist to DLQ table or dead letter topic
        }
    }

    /**
     * Get the number of pending events.
     */
    public int getPendingCount() {
        return pendingEvents.size();
    }

    /**
     * Domain event record for type-safe event publishing.
     */
    public record DomainEvent<T>(
        String eventType,
        String aggregateType,
        String aggregateId,
        T payload,
        Instant occurredAt,
        Map<String, String> metadata
    ) {
        public static <T> DomainEvent<T> of(String eventType, String aggregateType,
                                            String aggregateId, T payload) {
            return new DomainEvent<>(eventType, aggregateType, aggregateId,
                payload, Instant.now(), Map.of());
        }
    }
}
