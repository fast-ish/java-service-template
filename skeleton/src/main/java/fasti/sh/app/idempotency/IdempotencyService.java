package fasti.sh.app.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling idempotent requests.
 *
 * Ensures that duplicate requests with the same idempotency key
 * return the same response without re-executing the operation.
 *
 * For production, replace the in-memory store with Redis or database.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final ObjectMapper objectMapper;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    // In-memory store - replace with Redis/DB for production
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    public IdempotencyService(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.cacheHitCounter = Counter.builder("idempotency.cache.hits")
            .description("Number of idempotency cache hits")
            .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("idempotency.cache.misses")
            .description("Number of idempotency cache misses")
            .register(meterRegistry);
    }

    /**
     * Check if an idempotency key exists and return cached response if available.
     */
    public Optional<CachedResponse> checkIdempotency(String key, String requestHash) {
        IdempotencyRecord record = store.get(key);

        if (record == null) {
            cacheMissCounter.increment();
            return Optional.empty();
        }

        // Check if expired
        if (record.expiresAt().isBefore(Instant.now())) {
            store.remove(key);
            cacheMissCounter.increment();
            return Optional.empty();
        }

        // Verify request hash matches (same key, same request body)
        if (!record.requestHash().equals(requestHash)) {
            log.warn("Idempotency key {} reused with different request body", key);
            throw new IdempotencyConflictException(
                "Idempotency key already used with a different request");
        }

        cacheHitCounter.increment();

        // If still processing, client should retry
        if (record.status() == IdempotencyStatus.PROCESSING) {
            return Optional.of(new CachedResponse(null, 409, true));
        }

        return Optional.of(new CachedResponse(record.response(), record.statusCode(), false));
    }

    /**
     * Start processing an idempotent request.
     */
    public void startProcessing(String key, String requestHash, Duration ttl) {
        IdempotencyRecord record = new IdempotencyRecord(
            key,
            requestHash,
            IdempotencyStatus.PROCESSING,
            null,
            0,
            Instant.now(),
            Instant.now().plus(ttl)
        );
        store.put(key, record);
        log.debug("Started idempotent processing for key: {}", key);
    }

    /**
     * Complete processing and cache the response.
     */
    public void completeProcessing(String key, Object response, int statusCode) {
        IdempotencyRecord existing = store.get(key);
        if (existing == null) {
            log.warn("Completing processing for unknown idempotency key: {}", key);
            return;
        }

        String responseJson = serializeResponse(response);
        IdempotencyRecord completed = new IdempotencyRecord(
            key,
            existing.requestHash(),
            IdempotencyStatus.COMPLETED,
            responseJson,
            statusCode,
            existing.createdAt(),
            existing.expiresAt()
        );
        store.put(key, completed);
        log.debug("Completed idempotent processing for key: {}", key);
    }

    /**
     * Mark processing as failed.
     */
    public void failProcessing(String key) {
        store.remove(key);
        log.debug("Failed idempotent processing for key: {}", key);
    }

    /**
     * Compute hash of request body for validation.
     */
    public String computeRequestHash(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute request hash", e);
            return "";
        }
    }

    private String serializeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
            return "{}";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Records
    public record IdempotencyRecord(
        String key,
        String requestHash,
        IdempotencyStatus status,
        String response,
        int statusCode,
        Instant createdAt,
        Instant expiresAt
    ) {}

    public record CachedResponse(
        String response,
        int statusCode,
        boolean stillProcessing
    ) {}

    public enum IdempotencyStatus {
        PROCESSING, COMPLETED, FAILED
    }

    public static class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String message) {
            super(message);
        }
    }
}
