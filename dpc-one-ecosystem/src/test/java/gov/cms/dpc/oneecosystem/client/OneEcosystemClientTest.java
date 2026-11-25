package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusResponse;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.awaitility.Awaitility.await;
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

        // update this when we can move to other api calls and check for a valid response
        assertEquals(400, statusCode,
                "Should show client has authenticated but received an error");
    }

    @Test
    void testExport() {
        OneEcosystemExportRequest request = new OneEcosystemExportRequest(
                List.of("1S00EU7JJ00", "1S00EU7JJ11", "1S00EU7JJ18", "1S00EU8EX75", "1S00EU8EY44"),
                Instant.parse("2026-08-01T13:00:00Z"), // 2026-08-01T08:00:00-05:00
                List.of("Patient", "ExplanationOfBenefit", "Coverage"),
                "https://example.com/api/v3/Group/1234/$export"
        );

        OneEcosystemExportResponse response = client.export(request);

        assertEquals(202, response.statusCode(),
                "Export request should be accepted for async processing");
        assertNotNull(response.contentLocation(),
                "Should receive a location (full URL or job ID) for the export job");
        String jobUrl = response.contentLocation();
        assertTrue(jobUrl.matches("https?://.+/api/v3/jobs/[^/]+"),
                "Expected a job URL ending in a job ID: " + jobUrl);


        String jobId = jobUrl.substring(jobUrl.lastIndexOf('/') + 1);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(50))
                .until(() -> {
                    var jobStatus = client.jobStatus(jobId);
                    if (jobStatus.statusCode() == 202) {
                        return false;
                    }
                    assertEquals(200, jobStatus.statusCode());
                    assertJobStatusBody(jobStatus.body(), jobId, request.resourceTypes());
                    return true;
                });
    }

    private void assertJobStatusBody(OneEcosystemJobStatusResponse body, String jobId, List<String> resourceTypes) {
        assertNotNull(body, "Completed job should have a response body");
        assertEquals(jobId, body.JobID());
        assertNotNull(body.transactionTime());
        assertEquals(List.of(), body.error(), "Completed job should not contain export errors");

        for (var output : body.output()) {
            assertTrue(resourceTypes.contains(output.type()), "Output should have a requested resource type");
            assertFalse(output.url().isBlank(), "Output URL should not be blank");
        }
    }
}
