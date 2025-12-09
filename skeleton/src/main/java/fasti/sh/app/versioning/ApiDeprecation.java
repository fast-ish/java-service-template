package fasti.sh.app.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an API endpoint as deprecated.
 *
 * Usage:
 * <pre>
 * @GetMapping("/users")
 * @ApiDeprecation(
 *     since = "2024-01-01",
 *     sunset = "2024-06-01",
 *     successor = "/api/v2/users",
 *     message = "Use v2 API for improved pagination"
 * )
 * public ResponseEntity<List<User>> listUsers() {
 *     // Deprecated endpoint
 * }
 * </pre>
 *
 * This will add HTTP headers:
 * - Deprecation: @1704067200
 * - Sunset: Sat, 01 Jun 2024 00:00:00 GMT
 * - Link: </api/v2/users>; rel="successor-version"
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDeprecation {

    /**
     * ISO date when the API was deprecated (e.g., "2024-01-01").
     */
    String since();

    /**
     * ISO date when the API will be removed (e.g., "2024-06-01").
     */
    String sunset();

    /**
     * URL of the successor endpoint.
     */
    String successor() default "";

    /**
     * Human-readable deprecation message.
     */
    String message() default "";

    /**
     * Whether to log usage of deprecated endpoints.
     */
    boolean logUsage() default true;
}
