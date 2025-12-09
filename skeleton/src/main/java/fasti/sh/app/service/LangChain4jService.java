package fasti.sh.app.service;

{%- if values.aiClient == "langchain4j" %}
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage as LcUserMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LangChain4j-based AI service with built-in tooling support.
 *
 * Features:
 * - Chat completions with any configured provider
 * - AI Services for declarative AI interactions
 * - Tool/Function calling support
 * - RAG-ready with embedding support
 */
@Service
public class LangChain4jService {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jService.class);

    private final ChatLanguageModel chatModel;
    private final MeterRegistry meterRegistry;
    private final AssistantService assistant;

    public LangChain4jService(ChatLanguageModel chatModel, MeterRegistry meterRegistry) {
        this.chatModel = chatModel;
        this.meterRegistry = meterRegistry;
        // Create AI Service proxy for declarative interactions
        this.assistant = AiServices.create(AssistantService.class, chatModel);
    }

    /**
     * Simple chat completion.
     */
    public String chat(String userMessage) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.debug("Processing chat request");
            Response<AiMessage> response = chatModel.generate(UserMessage.from(userMessage));
            return response.content().text();
        } finally {
            sample.stop(Timer.builder("ai.chat.duration")
                .tag("client", "langchain4j")
                .register(meterRegistry));
        }
    }

    /**
     * Chat with system context.
     */
    public String chatWithContext(String systemPrompt, String userMessage) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.debug("Processing chat request with system context");
            Response<AiMessage> response = chatModel.generate(
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)
            );
            return response.content().text();
        } finally {
            sample.stop(Timer.builder("ai.chat.duration")
                .tag("client", "langchain4j")
                .tag("type", "with_context")
                .register(meterRegistry));
        }
    }

    /**
     * Use the declarative AI Service for structured interactions.
     */
    public String summarize(String text) {
        return assistant.summarize(text);
    }

    public String translate(String text, String targetLanguage) {
        return assistant.translate(text, targetLanguage);
    }

    public String extractEntities(String text) {
        return assistant.extractEntities(text);
    }

    /**
     * Declarative AI Service interface.
     * LangChain4j generates the implementation at runtime.
     */
    interface AssistantService {

        @SystemMessage("You are a helpful assistant that summarizes text concisely.")
        String summarize(@LcUserMessage String text);

        @SystemMessage("You are a professional translator. Translate the following text accurately.")
        String translate(@LcUserMessage String text, String targetLanguage);

        @SystemMessage("Extract named entities (people, places, organizations, dates) from the text. Return as JSON.")
        String extractEntities(@LcUserMessage String text);
    }
}
{%- else %}
import org.springframework.stereotype.Service;

/**
 * Placeholder for LangChain4j service.
 * Enable by selecting 'langchain4j' as the AI client.
 */
@Service
public class LangChain4jService {
    // LangChain4j not enabled for this service
}
{%- endif %}
