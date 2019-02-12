package gov.cms.dpc.web;

import ca.uhn.fhir.parser.IParser;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.web.resources.v1.GroupResource;
import gov.cms.dpc.web.resources.v1.JobResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * Verifies the a user can successfully submit a data export job
 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class FHIRSubmissionTest {
    private static final IParser parser = mock(IParser.class);
    public static final String FHIR_JSON = "application/fhir+json";
    private final JobQueue queue = new MemoryQueue();
    private ResourceExtension groupResource = ResourceExtension.builder().addResource(new GroupResource(parser, queue)).build();
    private ResourceExtension jobResource = ResourceExtension.builder().addResource(new JobResource(queue)).build();


    // This is required for Guice to load correctly. Not entirely sure why
    // https://github.com/dropwizard/dropwizard/issues/1772
    @BeforeAll
    public static void setup() {
        JerseyGuiceUtils.reset();
    }

    @AfterEach
    public void teardownTest() {
        reset(parser);
    }

    @Test
    public void testDataRequest() {

        final WebTarget target = groupResource.client().target("/Group/1/$export");
        target.request().accept(FHIR_JSON);
        final Response response = target.request().get();
        assertAll(() -> assertEquals(HttpStatus.NO_CONTENT_204, response.getStatus(), "Should have 204 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Check that the job is in progress

        String jobURL = response.getHeaderString("Content-Location").replace("http://localhost:3002/v1", "");
        WebTarget jobTarget = jobResource.client().target(jobURL);
        Response jobResp = jobTarget.request().accept("application/fhir+json").get();
        assertEquals(HttpStatus.ACCEPTED_202, jobResp.getStatus(), "Job should be in progress");

        // Finish the job and check again
        assertEquals(1, queue.queueSize(), "Should have at least one job in queue");
        queue.completeJob(queue.workJob().orElseThrow(() -> new IllegalStateException("Should have a job")).getLeft(), JobStatus.COMPLETED);

        jobTarget = jobResource.client().target(jobURL);
        jobResp = jobTarget.request().accept("application/fhir+json").get();
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
