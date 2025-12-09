package fasti.sh.app.service;

{%- if values.aiClient == "spring-ai" %}
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient.Builder chatClientBuilder;

    @Timed(value = "ai.chat", description = "Time taken for AI chat completion")
    public String chat(String userMessage) {
        log.info("Processing AI chat request");

        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();

        log.info("AI chat completed successfully");
        return response;
    }

    @Timed(value = "ai.chat.system", description = "Time taken for AI chat with system prompt")
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        log.info("Processing AI chat request with system prompt");

        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content();

        log.info("AI chat with system prompt completed successfully");
        return response;
    }
}
{%- elif values.aiClient == "openai" %}
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private OpenAIClient client;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
            log.info("OpenAI client initialized with model: {}", model);
        } else {
            log.warn("OpenAI API key not configured");
        }
    }

    @Timed(value = "ai.chat", description = "Time taken for AI chat completion")
    public String chat(String userMessage) {
        if (client == null) {
            throw new IllegalStateException("OpenAI client not initialized - API key not configured");
        }

        log.info("Processing OpenAI chat request");

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(model)
            .addMessage(ChatCompletionUserMessageParam.builder()
                .content(userMessage)
                .build())
            .build();

        ChatCompletion completion = client.chat().completions().create(params);

        String response = completion.choices().get(0).message().content().orElse("");
        log.info("OpenAI chat completed successfully");
        return response;
    }
}
{%- elif values.aiClient == "anthropic" %}
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.Message;
import com.anthropic.models.MessageCreateParams;
import com.anthropic.models.Model;
import com.anthropic.models.TextBlock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String model;

    private AnthropicClient client;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
            log.info("Anthropic client initialized with model: {}", model);
        } else {
            log.warn("Anthropic API key not configured");
        }
    }

    @Timed(value = "ai.chat", description = "Time taken for AI chat completion")
    public String chat(String userMessage) {
        if (client == null) {
            throw new IllegalStateException("Anthropic client not initialized - API key not configured");
        }

        log.info("Processing Anthropic chat request");

        MessageCreateParams params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(1024)
            .addUserMessage(userMessage)
            .build();

        Message message = client.messages().create(params);

        String response = message.content().stream()
            .filter(block -> block instanceof TextBlock)
            .map(block -> ((TextBlock) block).text())
            .findFirst()
            .orElse("");

        log.info("Anthropic chat completed successfully");
        return response;
    }
}
{%- elif values.aiClient == "bedrock" %}
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Slf4j
@Service
public class AiService {

    @Value("${aws.bedrock.region:us-east-1}")
    private String region;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;

    private BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(region))
            .build();
        log.info("AWS Bedrock client initialized with model: {}", modelId);
    }

    @Timed(value = "ai.chat", description = "Time taken for AI chat completion")
    public String chat(String userMessage) {
        log.info("Processing Bedrock chat request");

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("max_tokens", 1024);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", userMessage);

            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(requestBody)))
                .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            JsonNode responseBody = objectMapper.readTree(
                response.body().asString(StandardCharsets.UTF_8));

            String result = responseBody.path("content").get(0).path("text").asText();
            log.info("Bedrock chat completed successfully");
            return result;

        } catch (Exception e) {
            log.error("Bedrock chat failed", e);
            throw new RuntimeException("Failed to process Bedrock request", e);
        }
    }
}
{%- else %}
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {

    public String chat(String userMessage) {
        log.warn("AI client not configured - returning echo response");
        return "AI client not configured. Message received: " + userMessage;
    }
}
{%- endif %}
