package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Disabled because we don't want to run as part of CI until we have environments set up
 *
 * manually test against dev w/ these commands:
 *   make secure-envs
 *   source ops/config/decrypted/local.env
 *   export ONE_ECOSYSTEM_CLIENT_ID ONE_ECOSYSTEM_CLIENT_SECRET
 *   make one-ecosystem-integration-tests
 */
@Disabled
@IntegrationTest
class OneEcosystemClientTest {

    private static OneEcosystemClientImpl client;

    @BeforeAll
    static void setupOneEcosystemClient() throws IOException, ConfigurationException {
        client = new OneEcosystemClientImpl(OneEcosystemTestHelpers.loadConfig("dev.application.yml"), FhirContext.forR4());
    }

    @Test
    void testLegacyEndpointAuthenticated() {
        int statusCode = client.callLegacyEndpoint();
        assertNotEquals(401, statusCode, "should not get a 401");

        // update this when we can move to other api calls and check for a valid response
        assertEquals(400, statusCode,
                "Should show client has authenticated but received an error");
    }
}
