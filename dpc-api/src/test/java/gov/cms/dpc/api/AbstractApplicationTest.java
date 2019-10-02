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
 * Default application setup the runs the {@link DPCAPIService} with authentication disabled. (e.g. using the {@link gov.cms.dpc.api.auth.StaticAuthFilter}
 */
@IntegrationTest
public class AbstractApplicationTest {

    // Application prefix, which we need in order to correctly override config values.
    private static final String KEY_PREFIX = "dpc.api";

    protected static final DropwizardTestSupport<DPCAPIConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAPIService.class, null,
            ConfigOverride.config(KEY_PREFIX, "authenticationDisabled", "true"));
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


