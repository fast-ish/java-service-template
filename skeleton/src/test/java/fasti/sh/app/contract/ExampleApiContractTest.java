package fasti.sh.app.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pact Provider Contract Test.
 *
 * This test verifies that our API implementation matches the contracts
 * defined by our consumers.
 *
 * Contracts can be loaded from:
 * 1. Local files (pacts folder) - use @PactFolder
 * 2. Pact Broker - use @PactBroker
 *
 * Usage:
 * 1. Consumers generate contracts using Pact Consumer tests
 * 2. Contracts are published to Pact Broker (or shared as files)
 * 3. This test verifies our provider implementation matches those contracts
 *
 * Run with: mvn test -Dpact.verifier.publishResults=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Provider("${{values.name}}")
// Load contracts from local folder for development
@PactFolder("pacts")
// Uncomment for Pact Broker integration:
// @PactBroker(
//     url = "${PACT_BROKER_URL}",
//     authentication = @PactBrokerAuth(token = "${PACT_BROKER_TOKEN}")
// )
public class ExampleApiContractTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // State handlers - set up test data for specific contract states

    @State("example exists with id 1")
    public void exampleExistsWithId1() {
        // Set up test data
        // exampleService.create("Test Example", "Test Description");
    }

    @State("no examples exist")
    public void noExamplesExist() {
        // Clear test data
    }

    @State("example list is not empty")
    public void exampleListIsNotEmpty() {
        // Create some test examples
    }

    // Add more state handlers as needed for your contracts
}
