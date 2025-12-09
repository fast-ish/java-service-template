package fasti.sh.app.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import fasti.sh.app.dto.ExampleRequest;
import fasti.sh.app.dto.ExampleResponse;
import fasti.sh.app.dto.PageResponse;
import fasti.sh.app.exception.BadRequestException;
import fasti.sh.app.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Example service demonstrating golden path patterns:
 * - Constructor injection
 * - Custom metrics
 * - Proper exception handling
 * - Logging best practices
 * - Timed methods for observability
 */
@Slf4j
@Service
public class ExampleService {

    private final Map<Long, ExampleEntity> dataStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    private final Counter createCounter;
    private final Counter deleteCounter;

    public ExampleService(MeterRegistry meterRegistry) {
        this.createCounter = Counter.builder("example.entities.created")
            .description("Number of example entities created")
            .register(meterRegistry);
        this.deleteCounter = Counter.builder("example.entities.deleted")
            .description("Number of example entities deleted")
            .register(meterRegistry);
    }

    @Timed(value = "service.example.create", description = "Time to create an entity")
    public ExampleEntity create(String name, String description) {
        log.info("Creating new entity with name: {}", name);

        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }

        long id = idGenerator.getAndIncrement();
        ExampleEntity entity = new ExampleEntity(id, name, description);
        dataStore.put(id, entity);

        createCounter.increment();
        log.info("Created entity with id: {}", id);
        return entity;
    }

    @Timed(value = "service.example.findById", description = "Time to find entity by id")
    public ExampleEntity findById(Long id) {
        log.debug("Finding entity by id: {}", id);

        return Optional.ofNullable(dataStore.get(id))
            .orElseThrow(() -> new ResourceNotFoundException("Entity", "id", id));
    }

    @Timed(value = "service.example.findAll", description = "Time to find all entities")
    public PageResponse<ExampleEntity> findAll(int page, int size) {
        log.debug("Finding all entities, page: {}, size: {}", page, size);

        List<ExampleEntity> all = dataStore.values().stream().toList();
        int start = page * size;
        int end = Math.min(start + size, all.size());

        List<ExampleEntity> pageContent = start < all.size()
            ? all.subList(start, end)
            : List.of();

        return PageResponse.of(pageContent, page, size, all.size());
    }

    @Timed(value = "service.example.update", description = "Time to update an entity")
    public ExampleEntity update(Long id, String name, String description) {
        log.info("Updating entity with id: {}", id);

        ExampleEntity existing = findById(id);
        ExampleEntity updated = new ExampleEntity(
            existing.id(),
            name != null ? name : existing.name(),
            description != null ? description : existing.description()
        );

        dataStore.put(id, updated);
        log.info("Updated entity with id: {}", id);
        return updated;
    }

    @Timed(value = "service.example.delete", description = "Time to delete an entity")
    public void delete(Long id) {
        log.info("Deleting entity with id: {}", id);

        if (!dataStore.containsKey(id)) {
            throw new ResourceNotFoundException("Entity", "id", id);
        }

        dataStore.remove(id);
        deleteCounter.increment();
        log.info("Deleted entity with id: {}", id);
    }

    public record ExampleEntity(Long id, String name, String description) {}

    // DTO-based methods for versioned API

    @Timed(value = "service.example.getById", description = "Time to get entity by id")
    public ExampleResponse getById(String id) {
        log.debug("Getting entity by id: {}", id);

        return dataStore.values().stream()
            .filter(e -> e.id().toString().equals(id))
            .findFirst()
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Entity", "id", id));
    }

    @Timed(value = "service.example.createFromRequest", description = "Time to create entity from request")
    public ExampleResponse create(ExampleRequest request) {
        log.info("Creating new entity with name: {}", request.name());

        long id = idGenerator.getAndIncrement();
        ExampleEntity entity = new ExampleEntity(id, request.name(), request.description());
        dataStore.put(id, entity);

        createCounter.increment();
        log.info("Created entity with id: {}", id);
        return toResponse(entity);
    }

    private ExampleResponse toResponse(ExampleEntity entity) {
        return new ExampleResponse(
            entity.id().toString(),
            entity.name(),
            entity.description(),
            Instant.now(),
            Instant.now()
        );
    }
}
