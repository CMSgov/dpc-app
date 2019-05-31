package gov.cms.dpc.api;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.queue.models.JobModel;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

import static gov.cms.dpc.api.client.ClientUtils.ATTRIBUTION_CSV;
import static org.junit.jupiter.api.Assertions.*;

class ManualExportRequestTest extends AbstractApplicationTest {

    /**
     * This test verifies the E2E flow of the manual export request process.
     * The test performs the following actions:
     * 1. Request data for a provider which does not exist and which does not have any patients associated with it.
     * 2. Submit a roster with a set of attributed patients (from the seeds file)
     * 3. Resubmit the request and received a job code
     * 4. Monitor for the job to complete and then retrieve the data
     * 5. Verifies that the downloaded file contains the necessary number of patients (2)
     */
    @Test
    void manuallyExportPatients() throws IOException, InterruptedException {
        // Create a group to export
        final Group group = generateGroup();

        // Make the POST request to the export endpoint with the provided group
        final HttpPost post = new HttpPost(getBaseURL() + "/Group/1/$export");
        final IParser parser = ctx.newJsonParser();
        post.setEntity(new StringEntity(parser.encodeResourceToString(group)));

        final CloseableHttpClient client = HttpClients.createDefault();

        try (CloseableHttpResponse response = client.execute(post)) {
            assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatusLine().getStatusCode(), "Should have passed");
            final OperationOutcome outcome = (OperationOutcome) parser.parseResource(EntityUtils.toString(response.getEntity()));
            assertEquals(2, outcome.getIssue().size(), "Should have 2 401s");
        }

        // Gather the patient IDs and submit them to the roster
        final InputStream resource = EndToEndRequestTest.class.getClassLoader().getResourceAsStream(ATTRIBUTION_CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", EndToEndRequestTest.class.getName(), ATTRIBUTION_CSV);
        }

        final IGenericClient rosterClient = ctx.newRestfulGenericClient(getBaseURL());
        final ICreateTyped rosterSubmission = ClientUtils.createRosterSubmission(rosterClient, resource);
        rosterSubmission.execute();

        // Try the export again
        String jobLocation;
        try (CloseableHttpResponse response = client.execute(post)) {
            assertEquals(HttpStatus.NO_CONTENT_204, response.getStatusLine().getStatusCode(), "Should have passed");

            // Get the 'Content-Location' header, which has the job URL
            jobLocation = response.getHeaders(HttpHeader.CONTENT_LOCATION.toString())[0].getElements()[0].getName();
        }

        final JobCompletionModel jobResponse = ClientUtils.awaitExportResponse(jobLocation, "Trying");


        assertAll(() -> assertNotNull(jobResponse, "Should have Job Response"),
                () -> assertEquals(JobModel.validResourceTypes.size(), jobResponse.getOutput().size(), "Should have all resource files"),
                () -> assertEquals(0, jobResponse.getError().size(), "Should not have any errors"));

        // Validate each of the resources
        validateResourceFile(Patient.class, jobResponse, ResourceType.Patient, 2);
        // EOBs are structured as bundles, even though they have the EOB resource type
        validateResourceFile(Bundle.class, jobResponse, ResourceType.ExplanationOfBenefit, 2);
        // Coverages are structured as bundles of Coverages
        validateResourceFile(Bundle.class, jobResponse, ResourceType.Coverage, 2);
        assertThrows(IllegalStateException.class, () -> validateResourceFile(Schedule.class, jobResponse, ResourceType.Schedule, 0), "Should not have a schedule response");
    }

    private Group generateGroup() {
        final Group group = new Group();
        // Set the provider ID
        group.addIdentifier().setValue(ClientUtils.PROVIDER_ID);

        // Add 2 patients which are not attributed to the provider
        group
                .addMember()
                .setEntity(new Reference().setIdentifier(new Identifier().setValue("20000000000888")));
        group
                .addMember()
                .setEntity(new Reference().setIdentifier(new Identifier().setValue("20000000000889")));

        return group;
    }
}
