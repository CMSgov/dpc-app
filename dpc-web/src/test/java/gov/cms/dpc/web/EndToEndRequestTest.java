package gov.cms.dpc.web;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import gov.cms.dpc.common.utils.SeedProcessor;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EndToEndRequestTest extends AbstractApplicationTest {

    private static final String PROVIDER_ID = "8D80925A-027E-43DD-8AED-9A501CC4CD91";
    private static final String CSV = "test_associations.csv";

    /**
     * This test verifies the E2E flow of the application.
     * The user performs the following actions:
     * 1. Request data for a provider which does not exist (receive error)
     * 2. Submit a roster with a set of attributed patients (from the seeds file)
     * 3. Resubmit the request and received a job code
     * 4. Monitor for the job to complete and then retrieve the data
     */
    @Test
    public void simpleRequestWorkflow() throws IOException {

        // Submit an export request for a provider which is not known to the system.
        final IGenericClient exportClient = ctx.newRestfulGenericClient(getBaseURL());

        final IOperationUntypedWithInput<Parameters> exportOperation = exportClient
                .operation()
                .onInstance(new IdDt("Group", PROVIDER_ID))
                .named("$export")
                .withNoParameters(Parameters.class)
                .encodedJson()
                .useHttpGet();

//        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, exportOperation::execute);
//
//        // Extract the operation outcome, to make validation easier
//        final OperationOutcome outcome = (OperationOutcome) thrown.getOperationOutcome();
//        final OperationOutcome.OperationOutcomeIssueComponent firstIssue = outcome.getIssueFirstRep();
//
//        assertAll(() -> assertEquals(HttpStatus.NOT_FOUND_404, thrown.getStatusCode(), "Should not have found provider"),
//                () -> assertEquals("fatal", firstIssue.getSeverity().toCode(), "Should be a fatal error"),
//                () -> assertEquals(1, outcome.getIssue().size(), "Should only have a single error"));

        // Now, submit the roster and try again.

        final InputStream resource = EndToEndRequestTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", EndToEndRequestTest.class.getName(), CSV);
        }

        final SeedProcessor seedProcessor = new SeedProcessor(resource);

        final Map<String, List<Pair<String, String>>> providerMap = seedProcessor.extractProviderMap();

        // Find the entry for the given key (yes, I know this is bad)
        final Map.Entry<String, List<Pair<String, String>>> providerRoster = providerMap
                .entrySet()
                .stream()
                .filter((entry) -> entry.getKey().equals(PROVIDER_ID))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find matching provider"));

        final Bundle providerBundle = seedProcessor.generateRosterBundle(providerRoster);

        // Now, submit the bundle
        final IGenericClient rosterClient = ctx.newRestfulGenericClient(getBaseURL());

//        rosterClient.create().resource(providerBundle).ex

        final MethodOutcome rosterOutcome = rosterClient
                .create()
                .resource(providerBundle)
                .encodedJson()
                .execute();

//        assertTrue(rosterOutcome.getCreated(), "Should have created the roster");

        // Try the export request again
        final NonFhirResponseException exportThrown = assertThrows(NonFhirResponseException.class, exportOperation::execute);
        // Verify 204
        assertEquals(HttpStatus.NO_CONTENT_204, exportThrown.getStatusCode(), "Should have succeeded with no content");
        final Map<String, List<String>> headers = exportThrown.getResponseHeaders();

        // Get the headers and check the status
        final String jobLocation = headers.get(HttpHeaders.CONTENT_TYPE).get(0);

        final IGenericClient iGenericClient = ctx.newRestfulGenericClient(getBaseURL());
    }
}
