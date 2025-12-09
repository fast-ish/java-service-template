# ADR-0003: AI Integration Options

## Status

Accepted

## Date

${{ '$now' | date('YYYY-MM-DD') }}

## Context

Modern services often need to integrate with Large Language Models (LLMs) for various use cases:
- Text generation and completion
- Summarization and translation
- Entity extraction
- Semantic search (RAG)
- Conversational interfaces

We need to provide flexible options for teams to integrate AI capabilities while maintaining consistency and observability.

## Decision Drivers

- Flexibility in provider choice (OpenAI, Anthropic, AWS Bedrock)
- Team familiarity with different approaches
- Feature requirements (basic chat vs advanced RAG/agents)
- Cost and billing management
- Observability and monitoring

## Considered Options

### Option 1: Spring AI

Spring's official AI abstraction layer.

**Pros:**
- Native Spring Boot integration
- Consistent API across providers
- Auto-configuration with starters
- Familiar to Spring developers

**Cons:**
- Relatively new (1.0 just released)
- Limited advanced features (agents, RAG)
- Fewer providers than alternatives

### Option 2: LangChain4j

Feature-rich AI toolkit for Java.

**Pros:**
- Full-featured: RAG, agents, tools, memory
- Many integrations (vector stores, embeddings)
- AI Services for declarative interfaces
- Active development and community

**Cons:**
- Steeper learning curve
- Larger dependency footprint
- Non-Spring-native (Spring support via starters)

### Option 3: Direct SDKs (OpenAI, Anthropic)

Use provider SDKs directly.

**Pros:**
- Full API access
- No abstraction overhead
- Latest features immediately

**Cons:**
- Vendor lock-in
- Inconsistent APIs across providers
- More boilerplate code

### Option 4: AWS Bedrock SDK

AWS-native AI integration.

**Pros:**
- Multi-model access (Claude, Llama, etc.)
- AWS IAM integration
- No API key management

**Cons:**
- AWS-only
- Less flexibility than direct APIs
- Pricing can be complex

## Decision

We offer **multiple options** to teams:

1. **Spring AI** (Recommended for simple use cases): Best for teams wanting Spring-native integration for basic chat/completion
2. **LangChain4j** (Recommended for advanced use cases): Best for RAG, agents, tools, and complex AI workflows
3. **Direct SDKs**: For teams needing full API control or latest features
4. **AWS Bedrock**: For teams preferring AWS-native without API key management

## Consequences

### Positive

- Teams can choose the right tool for their use case
- Consistent observability patterns across all options
- Easy to switch providers with abstraction layers

### Negative

- Multiple libraries to support
- Different patterns across options
- Documentation overhead

## Implementation Notes

### Spring AI
```java
@Autowired
private ChatClient chatClient;

String response = chatClient.prompt()
    .user("Hello!")
    .call()
    .content();
```

### LangChain4j
```java
// Declarative AI Service
interface Assistant {
    @SystemMessage("You are a helpful assistant")
    String chat(@UserMessage String message);
}

Assistant assistant = AiServices.create(Assistant.class, chatModel);
String response = assistant.chat("Hello!");
```

### Direct SDK (OpenAI)
```java
OpenAIClient client = OpenAIOkHttpClient.builder()
    .apiKey(apiKey)
    .build();

ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
    .model("gpt-4o")
    .addMessage(UserMessage.of("Hello!"))
    .build();
```

## References

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [OpenAI Java SDK](https://github.com/openai/openai-java)
- [Anthropic Java SDK](https://github.com/anthropics/anthropic-sdk-java)
