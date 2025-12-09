package fasti.sh.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fasti.sh.app.dto.ApiResponse;
import fasti.sh.app.dto.PageResponse;
import fasti.sh.app.service.ExampleService;
import fasti.sh.app.service.ExampleService.ExampleEntity;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Example REST controller demonstrating golden path patterns:
 * - Input validation with Jakarta Bean Validation
 * - Proper HTTP status codes
 * - Consistent response format with ApiResponse
 * - Pagination support
 * - Timed endpoints for observability
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping
    @Timed(value = "api.examples.list", description = "Time to list examples")
    public ResponseEntity<ApiResponse<PageResponse<ExampleEntity>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listing examples, page: {}, size: {}", page, size);

        PageResponse<ExampleEntity> result = exampleService.findAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Timed(value = "api.examples.get", description = "Time to get example by id")
    public ResponseEntity<ApiResponse<ExampleEntity>> get(@PathVariable Long id) {
        log.info("Getting example by id: {}", id);

        ExampleEntity entity = exampleService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(entity));
    }

    @PostMapping
    @Timed(value = "api.examples.create", description = "Time to create example")
    public ResponseEntity<ApiResponse<ExampleEntity>> create(
            @Valid @RequestBody CreateExampleRequest request) {
        log.info("Creating example with name: {}", request.getName());

        ExampleEntity entity = exampleService.create(request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(entity, "Example created successfully"));
    }

    @PutMapping("/{id}")
    @Timed(value = "api.examples.update", description = "Time to update example")
    public ResponseEntity<ApiResponse<ExampleEntity>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExampleRequest request) {
        log.info("Updating example with id: {}", id);

        ExampleEntity entity = exampleService.update(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.success(entity, "Example updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "api.examples.delete", description = "Time to delete example")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Deleting example with id: {}", id);

        exampleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Example deleted successfully"));
    }

    @Data
    public static class CreateExampleRequest {
        @NotBlank(message = "Name is required")
        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        private String name;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
    }

    @Data
    public static class UpdateExampleRequest {
        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        private String name;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
    }
}
