package fasti.sh.app.controller;

{%- if values.aiClient != "none" %}
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fasti.sh.app.dto.ApiResponse;
import fasti.sh.app.service.AiService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Timed(value = "api.ai.chat", description = "Time taken for AI chat endpoint")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received AI chat request");

        String response = aiService.chat(request.getMessage());

        return ResponseEntity.ok(ApiResponse.success(
            ChatResponse.builder()
                .message(response)
                .build()
        ));
    }

    @Data
    public static class ChatRequest {
        @NotBlank(message = "Message is required")
        private String message;
    }

    @Data
    @lombok.Builder
    public static class ChatResponse {
        private String message;
    }
}
{%- else %}
// AI Controller - not generated (aiClient = none)
// To enable AI endpoints, recreate the service with an AI client option.
{%- endif %}
