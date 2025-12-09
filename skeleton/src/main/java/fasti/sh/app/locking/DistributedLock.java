package fasti.sh.app.locking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply distributed locking to a method.
 *
 * Usage:
 * <pre>
 * @DistributedLock(
 *     key = "'order-processing-' + #orderId",
 *     waitTime = 5,
 *     leaseTime = 30
 * )
 * public void processOrder(String orderId) {
 *     // Only one instance can process this order at a time
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * SpEL expression for the lock key.
     * Can reference method parameters using #paramName.
     */
    String key();

    /**
     * Maximum time to wait for the lock (in seconds).
     */
    int waitTime() default 0;

    /**
     * Time after which the lock is automatically released (in seconds).
     */
    int leaseTime() default 30;

    /**
     * Whether to fail silently if lock cannot be acquired.
     */
    boolean failSilently() default false;
}
