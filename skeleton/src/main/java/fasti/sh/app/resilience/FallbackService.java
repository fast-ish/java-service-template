package fasti.sh.app.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Service for implementing graceful degradation patterns.
 *
 * Provides:
 * - Fallback caching for degraded mode
 * - Circuit breaker integration
 * - Degradation metrics
 */
@Service
public class FallbackService {

    private static final Logger log = LoggerFactory.getLogger(FallbackService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;

    // Fallback cache
    private final Map<String, CachedFallback<?>> fallbackCache = new ConcurrentHashMap<>();

    private final Counter fallbackUsedCounter;
    private final Counter cacheHitCounter;

    public FallbackService(CircuitBreakerRegistry circuitBreakerRegistry,
                          MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;

        this.fallbackUsedCounter = Counter.builder("graceful.degradation.fallback.used")
            .description("Number of times fallback was used")
            .register(meterRegistry);
        this.cacheHitCounter = Counter.builder("graceful.degradation.cache.hits")
            .description("Number of fallback cache hits")
            .register(meterRegistry);
    }

    /**
     * Execute with graceful degradation.
     *
     * @param name          Identifier for this degradation context
     * @param primaryAction The primary action to attempt
     * @param fallback      The fallback action if primary fails
     * @param cacheTtl      How long to cache successful results
     * @return Result from primary or fallback
     */
    public <T> T executeWithFallback(String name,
                                     Supplier<T> primaryAction,
                                     Supplier<T> fallback,
                                     Duration cacheTtl) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);

        try {
            // Try primary action with circuit breaker
            T result = circuitBreaker.executeSupplier(primaryAction);

            // Cache successful result for fallback
            if (cacheTtl != null && !cacheTtl.isZero()) {
                cacheResult(name, result, cacheTtl);
            }

            return result;

        } catch (Exception e) {
            log.warn("Primary action failed for {}, using fallback: {}", name, e.getMessage());
            return useFallback(name, fallback, e);
        }
    }

    /**
     * Execute with fallback, preferring cached value.
     */
    public <T> T executeWithCachedFallback(String name,
                                           Supplier<T> primaryAction,
                                           Supplier<T> fallback,
                                           Duration cacheTtl) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);

        try {
            T result = circuitBreaker.executeSupplier(primaryAction);
            cacheResult(name, result, cacheTtl);
            return result;

        } catch (Exception e) {
            log.warn("Primary action failed for {}, checking cache: {}", name, e.getMessage());

            // Try cached value first
            Optional<T> cached = getCachedResult(name);
            if (cached.isPresent()) {
                cacheHitCounter.increment();
                log.info("Using cached fallback for {}", name);
                return cached.get();
            }

            // Use provided fallback
            return useFallback(name, fallback, e);
        }
    }

    /**
     * Check if a service is in degraded mode.
     */
    public boolean isDegraded(String name) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN ||
               circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN;
    }

    /**
     * Get degradation status for all services.
     */
    public Map<String, DegradationStatus> getDegradationStatus() {
        Map<String, DegradationStatus> status = new ConcurrentHashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            status.put(cb.getName(), new DegradationStatus(
                cb.getName(),
                cb.getState().name(),
                cb.getMetrics().getFailureRate(),
                cb.getMetrics().getSlowCallRate(),
                cb.getMetrics().getNumberOfFailedCalls(),
                cb.getMetrics().getNumberOfSuccessfulCalls()
            ));
        });

        return status;
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getCachedResult(String name) {
        CachedFallback<?> cached = fallbackCache.get(name);
        if (cached != null && !cached.isExpired()) {
            return Optional.of((T) cached.value());
        }
        return Optional.empty();
    }

    private <T> void cacheResult(String name, T result, Duration ttl) {
        fallbackCache.put(name, new CachedFallback<>(result, Instant.now().plus(ttl)));
    }

    private <T> T useFallback(String name, Supplier<T> fallback, Exception originalError) {
        fallbackUsedCounter.increment();

        Counter.builder("graceful.degradation.errors")
            .tag("name", name)
            .tag("error", originalError.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();

        try {
            return fallback.get();
        } catch (Exception fallbackError) {
            log.error("Fallback also failed for {}: {}", name, fallbackError.getMessage());
            throw new DegradationException(
                "Both primary and fallback failed for: " + name, originalError);
        }
    }

    record CachedFallback<T>(T value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record DegradationStatus(
        String name,
        String state,
        float failureRate,
        float slowCallRate,
        long failedCalls,
        long successfulCalls
    ) {}

    public static class DegradationException extends RuntimeException {
        public DegradationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
