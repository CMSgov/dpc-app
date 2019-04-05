package gov.cms.dpc.api;

import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.api.client.AttributionServiceClient;
import gov.cms.dpc.api.resources.v1.GroupResource;
import gov.cms.dpc.api.resources.v1.JobResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies the a user can successfully submit a data export job
 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class FHIRSubmissionTest {
    public static final String TEST_BASE_URL = "http://localhost:3002/v1";
    private final JobQueue queue = spy(MemoryQueue.class);
    private final AttributionServiceClient client = mock(AttributionServiceClient.class);
    private ResourceExtension groupResource = ResourceExtension.builder().addResource(new GroupResource(queue, client, TEST_BASE_URL)).build();
    private ResourceExtension jobResource = ResourceExtension.builder().addResource(new JobResource(queue, TEST_BASE_URL)).build();


    // Test data
    private List<String> testBeneficiaries = List.of("1", "2", "3", "4");
    private final JobModel testJobModel = new JobModel(UUID.randomUUID(), JobModel.ResourceType.PATIENT, "1", testBeneficiaries);

    @BeforeEach()
    public void resetMocks() {
        reset(client);
        reset(queue);

        // Mock the attribution call
        Mockito.when(client.getAttributedPatientIDs(Mockito.any(Practitioner.class)))
                .thenReturn(Optional.of(testBeneficiaries));

        // Mock the submission call to verify the job type
        doAnswer((answer -> {
            final JobModel data = answer.getArgument(1);
            assertEquals(testJobModel.getPatients().size(), data.getPatients().size(), "Should have 4 beneies");
            return answer.callRealMethod();
        }))
                .when(queue).submitJob(Mockito.any(UUID.class), Mockito.any(JobModel.class));
    }

    @Test
    public void testDataRequest() {

        final WebTarget target = groupResource.client().target("/Group/1/$export");
        target.request().accept(FHIR_JSON);
        final Response response = target.request().get();
        assertAll(() -> assertEquals(HttpStatus.NO_CONTENT_204, response.getStatus(), "Should have 204 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Check that the job is in progress

        String jobURL = response.getHeaderString("Content-Location").replace(TEST_BASE_URL, "");
        WebTarget jobTarget = jobResource.client().target(jobURL);
        Response jobResp = jobTarget.request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.ACCEPTED_202, jobResp.getStatus(), "Job should be in progress");

        // Finish the job and check again
        assertEquals(1, queue.queueSize(), "Should have at least one job in queue");
        queue.completeJob(queue.workJob().orElseThrow(() -> new IllegalStateException("Should have a job")).getLeft(), JobStatus.COMPLETED);

        jobTarget = jobResource.client().target(jobURL);
        jobResp = jobTarget.request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.OK_200, jobResp.getStatus(), "Job should be done");


//        final IGenericClient client = ctx.newRestfulGenericClient();

//        final Parameters execute = client
//                .operation()
//                .onInstanceVersion(new IdDt("Group", "1"))
//                .named("$export")
//                .withNoParameters(Parameters.class)
//                .returnResourceType()
////                .encodedJson()
//                .useHttpGet()
//                .execute();
//
//        final Bundle responseBundle = (Bundle) execute.getParameter().get(0).getResource();
//
//        assertEquals(1, responseBundle.getTotal(), "Should only have 1 groupResource");
    }
}
