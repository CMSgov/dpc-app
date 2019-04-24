package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.annotations.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@IntegrationTest
public class AbstractApplicationTest {

    protected static final DropwizardTestSupport<DPCAPIConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAPIService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "7777"));
    public static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9901/tasks/truncate";
    protected FhirContext ctx;

    protected AbstractApplicationTest() {
    }

    protected String getBaseURL() {
        return String.format("http://localhost:%d/v1/", APPLICATION.getLocalPort());
    }

    @BeforeAll
    public static void setup() throws IOException {
        truncateDatabase();
        APPLICATION.before();
    }

    @BeforeEach
    public void createContext() {
        ctx = FhirContext.forDstu3();
    }

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
    }

    private static void truncateDatabase() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(ATTRIBUTION_TRUNCATE_TASK);

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should have truncated database");
            }
        }
    }

}


