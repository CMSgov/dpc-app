package gov.cms.dpc.oneecosystem.client;

import gov.cms.dpc.testing.IntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

 // mvn -pl dpc-one-ecosystem -Pintegration-tests -Dit.test=OneEcosystemClientDevTest -Dmaven.build.cache.enabled=false verify

/**
 * Disabled because we don't want to run as part of CI until we have environments set up
 * manually test w/ these commands:
 * make secure-envs
 * source ops/config/decrypted/local.env
 * export ONE_ECOSYSTEM_API_BASE_URL ONE_ECOSYSTEM_CLIENT_ID ONE_ECOSYSTEM_CLIENT_SECRET
 * make one-ecosystem-integration-tests
 */
@Disabled
@IntegrationTest
class OneEcosystemClientDevTest {

    @Test
    void testLegacyEndpointReturnsSuccess() throws Exception {
        OneEcosystemClientImpl client = new OneEcosystemClientImpl(
            System.getenv("ONE_ECOSYSTEM_API_BASE_URL"),
            System.getenv("ONE_ECOSYSTEM_CLIENT_ID"),
            System.getenv("ONE_ECOSYSTEM_CLIENT_SECRET")
        );

        try (var response = client.callLegacyEndpoint()) {
            int statusCode = response.getCode();
            assertTrue(statusCode != 403, "should not get a 403");

            // update this when we can move to other api calls and check for a valid response
            assertTrue(statusCode == 400,
                "Should show client has authenticated but received an error");
        }
    }
}
