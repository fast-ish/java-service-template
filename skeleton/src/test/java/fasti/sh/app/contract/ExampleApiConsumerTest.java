package fasti.sh.app.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact Consumer Contract Test.
 *
 * This test defines the contract expectations for an API we consume.
 * Run these tests to generate contract files that can be shared with providers.
 *
 * Usage:
 * 1. Define pacts describing expected API interactions
 * 2. Run tests to generate contract files in target/pacts
 * 3. Publish contracts to Pact Broker or share with provider team
 * 4. Provider runs verification tests against these contracts
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "example-provider")
public class ExampleApiConsumerTest {

    /**
     * Define the contract for getting an example by ID.
     */
    @Pact(consumer = "${{values.name}}")
    public V4Pact getExampleByIdPact(PactDslWithProvider builder) {
        DslPart responseBody = new PactDslJsonBody()
            .booleanValue("success", true)
            .stringType("message")
            .object("data")
                .stringType("id", "1")
                .stringType("name", "Test Example")
                .stringType("description", "Test Description")
                .datetime("createdAt", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .datetime("updatedAt", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .closeObject();

        return builder
            .given("example exists with id 1")
            .uponReceiving("a request for example with id 1")
            .path("/api/v1/examples/1")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    /**
     * Define the contract for creating an example.
     */
    @Pact(consumer = "${{values.name}}")
    public V4Pact createExamplePact(PactDslWithProvider builder) {
        DslPart requestBody = new PactDslJsonBody()
            .stringType("name", "New Example")
            .stringType("description", "New Description");

        DslPart responseBody = new PactDslJsonBody()
            .booleanValue("success", true)
            .stringValue("message", "Example created successfully")
            .object("data")
                .stringType("id")
                .stringValue("name", "New Example")
                .stringValue("description", "New Description")
                .datetime("createdAt", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .datetime("updatedAt", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .closeObject();

        return builder
            .uponReceiving("a request to create an example")
            .path("/api/v1/examples")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body(requestBody)
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    /**
     * Define the contract for not found response.
     */
    @Pact(consumer = "${{values.name}}")
    public V4Pact getExampleNotFoundPact(PactDslWithProvider builder) {
        DslPart responseBody = new PactDslJsonBody()
            .booleanValue("success", false)
            .stringValue("error", "NOT_FOUND")
            .stringType("message")
            .stringType("path")
            .datetime("timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSSX");

        return builder
            .given("no examples exist")
            .uponReceiving("a request for non-existent example")
            .path("/api/v1/examples/999")
            .method("GET")
            .willRespondWith()
            .status(404)
            .headers(Map.of("Content-Type", "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    // Test methods that verify against the mock server

    @Test
    @PactTestFor(pactMethod = "getExampleByIdPact")
    void testGetExampleById(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(
            mockServer.getUrl() + "/api/v1/examples/1",
            String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Test Example");
    }

    @Test
    @PactTestFor(pactMethod = "createExamplePact")
    void testCreateExample(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
            {
                "name": "New Example",
                "description": "New Description"
            }
            """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            mockServer.getUrl() + "/api/v1/examples",
            HttpMethod.POST,
            request,
            String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("New Example");
    }

    @Test
    @PactTestFor(pactMethod = "getExampleNotFoundPact")
    void testGetExampleNotFound(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.getForEntity(
                mockServer.getUrl() + "/api/v1/examples/999",
                String.class
            );
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertThat(e.getStatusCode().value()).isEqualTo(404);
        }
    }
}
