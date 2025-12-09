package fasti.sh.app.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable graceful degradation for a method.
 *
 * Usage:
 * <pre>
 * @GracefulDegradation(
 *     fallbackMethod = "getRecommendationsFallback",
 *     cacheKey = "'recommendations-' + #userId",
 *     cacheTtlMinutes = 60
 * )
 * public List<Product> getRecommendations(String userId) {
 *     // Call to ML service that might be slow/unavailable
 * }
 *
 * public List<Product> getRecommendationsFallback(String userId, Throwable t) {
 *     // Return cached/static recommendations
 *     return staticRecommendations;
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GracefulDegradation {

    /**
     * Name of the fallback method.
     * Must have the same parameters plus an optional Throwable parameter.
     */
    String fallbackMethod();

    /**
     * SpEL expression for cache key (optional).
     * If set, successful results are cached for fallback.
     */
    String cacheKey() default "";

    /**
     * Cache TTL in minutes.
     */
    int cacheTtlMinutes() default 60;

    /**
     * Whether to use cached value as fallback before calling fallbackMethod.
     */
    boolean useCachedFallback() default true;

    /**
     * Exceptions that should trigger fallback.
     */
    Class<? extends Throwable>[] fallbackFor() default {Exception.class};

    /**
     * Exceptions that should NOT trigger fallback.
     */
    Class<? extends Throwable>[] noFallbackFor() default {};
}
