package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.annotations.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

/**
 * Abstract test that enables the default token authentication backend.
 */
@IntegrationTest
public class AbstractSecureApplicationTest {
    protected static final String OTHER_ORG_ID = "065fbe84-3551-4ec3-98a3-0d1198c3cb55";
    // Application prefix, which we need in order to correctly override config values.
    private static final String KEY_PREFIX = "dpc.api";

    protected static final DropwizardTestSupport<DPCAPIConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAPIService.class, null,
            ConfigOverride.config(KEY_PREFIX, "", "true"));
    protected FhirContext ctx;

    protected AbstractSecureApplicationTest() {
        // Not used
    }

    protected String getBaseURL() {
        return String.format("http://localhost:%d/v1/", APPLICATION.getLocalPort());
    }

    @BeforeAll
    public static void setup() throws IOException {
        APITestHelpers.setupApplication(APPLICATION);
    }

    @BeforeEach
    public void eachSetup() throws IOException {
        ctx = FhirContext.forDstu3();

        // Check health
        APITestHelpers.checkHealth(APPLICATION);
    }

    @AfterEach
    public void eachShutdown() throws IOException {
        APITestHelpers.checkHealth(APPLICATION);
    }

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
    }
}
