package gov.cms.dpc.api;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.queue.models.JobModel;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndRequestTest extends AbstractApplicationTest {


    private static final String CSV = "test_associations.csv";

    /**
     * This test verifies the E2E flow of the application.
     * The test performs the following actions:
     * 1. Request data for a provider which does not exist (receive error)
     * 2. Submit a roster with a set of attributed patients (from the seeds file)
     * 3. Resubmit the request and received a job code
     * 4. Monitor for the job to complete and then retrieve the data
     * 5. Verifies that the downloaded file contains the necessary number of patients (100)
     */
    @Test
    public void simpleRequestWorkflow() throws IOException, InterruptedException {

        // Submit an export request for a provider which is not known to the system.
        final IGenericClient exportClient = ctx.newRestfulGenericClient(getBaseURL());

        final IOperationUntypedWithInput<Parameters> exportOperation = ClientUtils.createExportOperation(exportClient, ClientUtils.PROVIDER_ID);

        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, exportOperation::execute);

        // Extract the operation outcome, to make validation easier
        final OperationOutcome outcome = (OperationOutcome) thrown.getOperationOutcome();
        final OperationOutcome.OperationOutcomeIssueComponent firstIssue = outcome.getIssueFirstRep();

        assertAll(() -> assertEquals(HttpStatus.NOT_FOUND_404, thrown.getStatusCode(), "Should not have found provider"),
                () -> assertEquals("fatal", firstIssue.getSeverity().toCode(), "Should be a fatal error"),
                () -> assertEquals(1, outcome.getIssue().size(), "Should only have a single error"));

//         Now, submit the roster and try again.

        final InputStream resource = EndToEndRequestTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", EndToEndRequestTest.class.getName(), CSV);
        }

        final IGenericClient rosterClient = ctx.newRestfulGenericClient(getBaseURL());
        final ICreateTyped rosterSubmission = ClientUtils.createRosterSubmission(rosterClient, resource);
        rosterSubmission.execute();

        // Try the export request again
        final NonFhirResponseException exportThrown = assertThrows(NonFhirResponseException.class, exportOperation::execute);
        // Verify 204
        assertEquals(HttpStatus.NO_CONTENT_204, exportThrown.getStatusCode(), "Should have succeeded with no content");
        final Map<String, List<String>> headers = exportThrown.getResponseHeaders();

        // Get the headers and check the status
        final String jobLocation = headers.get("content-location").get(0);

        final JobCompletionModel jobResponse = ClientUtils.awaitExportResponse(jobLocation, "Trying");


        assertAll(() -> assertNotNull(jobResponse, "Should have Job Response"),
                () -> assertTrue(JobModel.validResourceTypes.size() <= jobResponse.getOutput().size(), "Should have at least one resource file per resource"),
                () -> assertEquals(0, jobResponse.getError().size(), "Should not have any errors"));

        // Validate each of the resources
        validateResourceFile(Patient.class, jobResponse, ResourceType.Patient, 100);
        // EOBs are structured as bundles, even though they have the EOB resource type
        validateResourceFile(ExplanationOfBenefit.class, jobResponse, ResourceType.ExplanationOfBenefit, 1000);
        // Coverages are structured as bundles of Coverages
        validateResourceFile(Coverage.class, jobResponse, ResourceType.Coverage, 300);
        assertThrows(IllegalStateException.class, () -> validateResourceFile(Schedule.class, jobResponse, ResourceType.Schedule, 0), "Should not have a schedule response");
    }

    private <T extends IBaseResource> void validateResourceFile(Class<T> clazz, JobCompletionModel response, ResourceType resourceType, int expectedSize) throws IOException {
        final String fileID = response
                .getOutput()
                .stream()
                .filter(output -> output.getType() == resourceType)
                .map(JobCompletionModel.OutputEntry::getUrl)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Should have at least 1 patient resource"));

        final File tempFile = ClientUtils.fetchExportedFiles(fileID);

        // Read the file back in and parse the patients
        final IParser parser = ctx.newJsonParser();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile, StandardCharsets.UTF_8))) {
            final List<T> entries = bufferedReader.lines()
                    .map((line) -> clazz.cast(parser.parseResource(line)))
                    .collect(Collectors.toList());

            assertEquals(expectedSize, entries.size(), String.format("Should have %d entries in the resource", expectedSize));
        }
    }
}

