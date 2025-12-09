package fasti.sh.app.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable audit logging for an endpoint.
 *
 * Usage:
 * <pre>
 * @PostMapping("/users")
 * @AuditLog(action = "CREATE_USER", resourceType = "USER")
 * public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
 *     // Request and response will be logged
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * The action being performed (e.g., CREATE, UPDATE, DELETE, ACCESS).
     */
    String action();

    /**
     * The type of resource being accessed.
     */
    String resourceType();

    /**
     * Whether to include request body in the log.
     */
    boolean includeRequestBody() default true;

    /**
     * Whether to include response body in the log.
     */
    boolean includeResponseBody() default false;

    /**
     * Fields to redact from request/response bodies.
     */
    String[] redactFields() default {"password", "secret", "token", "apiKey", "creditCard"};
}
