package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFilter;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_NPI;
import static gov.cms.dpc.testing.APIAuthHelpers.TASK_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Default application setup the runs the {@link DPCAPIService} with authentication disabled. (e.g. using the {@link StaticAuthFilter}
 */
@IntegrationTest
@ExtendWith(BufferedLoggerHandler.class)
public class AbstractApplicationTest {

    protected static final String configPath = "src/test/resources/ci.application.yml";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DropwizardTestSupport<DPCAPIConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCAPIService.class, configPath,
                    ConfigOverride.config("authenticationDisabled", "true"));
    protected FhirContext ctx;

    protected AbstractApplicationTest() {
        // Not used
    }

    protected String getBaseURL() {
        return String.format("http://localhost:%d/v1/", APPLICATION.getLocalPort());
    }

    @BeforeAll
    public static void setup() throws Exception {
        APITestHelpers.setupApplication(APPLICATION);
    }

    @BeforeEach
    public void eachSetup() throws IOException {
        ctx = FhirContext.forDstu3();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        FHIRHelpers.registerOrganization(attrClient,
                ctx.newJsonParser(),
                ORGANIZATION_ID,
                ORGANIZATION_NPI,
                TASK_URL);

        // Check health
        APITestHelpers.checkHealth(APPLICATION);
    }

    @AfterEach
    public void eachShutdown() throws IOException {
        APITestHelpers.checkHealth(APPLICATION);
    }

    @AfterAll
    public static void shutdown() throws IOException {
        checkAllConnectionsClosed(String.format("http://localhost:%s", APPLICATION.getAdminPort()));
        APPLICATION.after();
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


