package fasti.sh.app.dto;

import java.time.Instant;

/**
 * Response DTO for example resources.
 */
public record ExampleResponse(
    String id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
