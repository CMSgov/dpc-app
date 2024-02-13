package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.commons.lang3.tuple.Pair;
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
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_NPI;
import static gov.cms.dpc.testing.APIAuthHelpers.TASK_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Abstract test that enables the default token authentication backend.
 */
@IntegrationTest
@ExtendWith(BufferedLoggerHandler.class)
public class AbstractSecureApplicationTest {
    protected static final String OTHER_ORG_ID = "065fbe84-3551-4ec3-98a3-0d1198c3cb55";
    private static final String configPath = "src/test/resources/ci.application.yml";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DropwizardTestSupport<DPCAPIConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCAPIService.class, configPath);
    protected static FhirContext ctx;
    protected static String ORGANIZATION_TOKEN;
    // Macaroon to use for doing admin things (like creating tokens and keys)
    protected static String GOLDEN_MACAROON;
    protected static PrivateKey PRIVATE_KEY;
    protected static UUID PUBLIC_KEY_ID;

    protected AbstractSecureApplicationTest() {
        //not used
    }

    protected TestOrganizationContext registerAndSetupNewOrg() throws IOException, GeneralSecurityException, URISyntaxException {
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final String orgId = UUID.randomUUID().toString();
        final String npi = NPIUtil.generateNPI();
        final String clientToken = FHIRHelpers.registerOrganization(attrClient, ctx.newJsonParser(), orgId,  npi, TASK_URL);
        final Pair<UUID, PrivateKey> newKeyPair = APIAuthHelpers.generateAndUploadKey("integration-test-key", orgId, GOLDEN_MACAROON, "http://localhost:3002/v1/");
        return new TestOrganizationContext(clientToken,npi,orgId,newKeyPair.getLeft().toString(),newKeyPair.getRight());
    }

    protected String getBaseURL() {
        return String.format("http://localhost:%d/v1", APPLICATION.getLocalPort());
    }

    protected String getAdminURL() {
        return String.format("http://localhost:%d/tasks/", APPLICATION.getAdminPort());
    }

    @BeforeAll
    public static void setup() throws Exception {
        APITestHelpers.setupApplication(APPLICATION);
        ctx = FhirContext.forDstu3();
        // Register a test organization for us
        // First, create a Golden macaroon for admin uses
        GOLDEN_MACAROON = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        ORGANIZATION_TOKEN = FHIRHelpers.registerOrganization(attrClient, ctx.newJsonParser(), ORGANIZATION_ID, ORGANIZATION_NPI, TASK_URL);

        // Register Public key
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey("integration-test-key", ORGANIZATION_ID, GOLDEN_MACAROON, "http://localhost:3002/v1/");
        PRIVATE_KEY = uuidPrivateKeyPair.getRight();
        PUBLIC_KEY_ID = uuidPrivateKeyPair.getLeft();
    }

    @BeforeEach
    public void eachSetup() throws IOException {

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
