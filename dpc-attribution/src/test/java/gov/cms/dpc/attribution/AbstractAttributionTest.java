package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(BufferedLoggerHandler.class)
@IntegrationTest
public abstract class AbstractAttributionTest {
    protected static final String configPath = "src/test/resources/ci.application.yml";
    private static final ObjectMapper mapper = new ObjectMapper();

    protected static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCAttributionService.class, configPath);

    protected static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";

    protected FhirContext ctx = FhirContext.forDstu3();

    @BeforeAll
    public static void initDB() throws Exception {
        APPLICATION.before();
        SharedConfigurationState.clear();
        APPLICATION.getApplication().run("db", "migrate" , configPath);
        SharedConfigurationState.clear();
        APPLICATION.getApplication().run("seed", configPath);
    }

    @AfterAll
    public static void shutdown() throws IOException {
        // Ensure there are no active connections left
        checkAllConnectionsClosed(String.format("http://localhost:%s", APPLICATION.getAdminPort()));
        APPLICATION.after();
    }

    protected String getServerURL() {
        return String.format("http://localhost:%s/v1", APPLICATION.getLocalPort());
    }

    private static void checkAllConnectionsClosed(String adminURL) throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet metricsGet = new HttpGet(String.format("%s/metrics", adminURL));

            try (CloseableHttpResponse response = client.execute(metricsGet)) {
                final JsonNode metricsNode = mapper.reader().readTree(response.getEntity().getContent());
                metricsNode.get("gauges")
                        .fields()
                        .forEachRemaining(gauge -> {
                            if (gauge.getKey().matches("io.dropwizard.db.ManagedPooledDataSource\\..*\\.active")) {
                                final int activeConnections = gauge.getValue().asInt();
                                assertEquals(0, activeConnections, String.format("RESOURCE LEAK IN: %s. %d connections left open", gauge.getKey(), activeConnections));
                            }
                        });
            }
        }
    }
}
