package fasti.sh.app.controller.v1;

import fasti.sh.app.dto.ApiResponse;
import fasti.sh.app.dto.ExampleRequest;
import fasti.sh.app.dto.ExampleResponse;
import fasti.sh.app.service.ExampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example controller demonstrating API versioning via URL path.
 * Version 1 of the API.
 */
@RestController
@RequestMapping("/api/v1/examples")
@Tag(name = "Examples V1", description = "Example endpoints (API Version 1)")
public class ExampleControllerV1 {

    private final ExampleService exampleService;

    public ExampleControllerV1(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get example by ID", description = "Retrieves an example resource by its unique identifier")
    public ResponseEntity<ApiResponse<ExampleResponse>> getById(@PathVariable String id) {
        ExampleResponse response = exampleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create example", description = "Creates a new example resource")
    public ResponseEntity<ApiResponse<ExampleResponse>> create(@Valid @RequestBody ExampleRequest request) {
        ExampleResponse response = exampleService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Example created successfully"));
    }
}
