package fasti.sh.app.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable idempotency for an endpoint.
 *
 * Usage:
 * <pre>
 * @PostMapping("/orders")
 * @IdempotencyKey(header = "Idempotency-Key", ttlMinutes = 60)
 * public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
 *     // If same idempotency key is used, returns cached response
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotencyKey {

    /**
     * Header name containing the idempotency key.
     */
    String header() default "Idempotency-Key";

    /**
     * Time-to-live for the idempotency record in minutes.
     */
    int ttlMinutes() default 1440; // 24 hours

    /**
     * Whether the key is required. If true, returns 400 if missing.
     */
    boolean required() default false;
}
