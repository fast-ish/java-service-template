package fasti.sh.app.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an event in the transactional outbox.
 *
 * The outbox pattern ensures reliable event publishing by:
 * 1. Writing events to a database table in the same transaction as the business operation
 * 2. A separate process polls the outbox and publishes events to the message broker
 * 3. Events are marked as processed after successful publishing
 *
 * This guarantees at-least-once delivery even if the message broker is temporarily unavailable.
 */
public record OutboxEvent(
    UUID id,
    String aggregateType,
    String aggregateId,
    String eventType,
    String payload,
    Instant createdAt,
    Instant processedAt,
    int retryCount,
    String lastError
) {
    /**
     * Create a new outbox event.
     */
    public static OutboxEvent create(String aggregateType, String aggregateId,
                                     String eventType, String payload) {
        return new OutboxEvent(
            UUID.randomUUID(),
            aggregateType,
            aggregateId,
            eventType,
            payload,
            Instant.now(),
            null,
            0,
            null
        );
    }

    /**
     * Mark the event as processed.
     */
    public OutboxEvent markProcessed() {
        return new OutboxEvent(
            id, aggregateType, aggregateId, eventType, payload,
            createdAt, Instant.now(), retryCount, null
        );
    }

    /**
     * Mark the event as failed with error.
     */
    public OutboxEvent markFailed(String error) {
        return new OutboxEvent(
            id, aggregateType, aggregateId, eventType, payload,
            createdAt, null, retryCount + 1, error
        );
    }

    /**
     * Check if the event is processed.
     */
    public boolean isProcessed() {
        return processedAt != null;
    }

    /**
     * Check if the event should be retried.
     */
    public boolean shouldRetry(int maxRetries) {
        return !isProcessed() && retryCount < maxRetries;
    }
}
