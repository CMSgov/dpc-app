package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.StaticAuthFilter;
import gov.cms.dpc.api.auth.StaticAuthenticator;
import gov.cms.dpc.api.resources.v1.GroupResource;
import gov.cms.dpc.api.resources.v1.JobResource;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_HEADER;
import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_RESPOND_ASYNC;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies the a user can successfully submit a data export job
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@SuppressWarnings("rawtypes")
class FHIRSubmissionTest {
    private static final String TEST_BASE_URL = "http://localhost:3002/v1";
    private static final String TEST_PROVIDER_ID = "1";
    private static final JobQueue queue = spy(MemoryQueue.class);
    private static IGenericClient client = mock(IGenericClient.class);
    private static IRead mockRead = mock(IRead.class);
    private static IReadTyped mockTypedRead = mock(IReadTyped.class);
    private static IReadExecutable mockExecutable = mock(IReadExecutable.class);

    private static final AuthFilter<DPCAuthCredentials, OrganizationPrincipal> staticFilter = new StaticAuthFilter(new StaticAuthenticator());
    private static final GrizzlyWebTestContainerFactory testContainer = new GrizzlyWebTestContainerFactory();

    // Test data
    private static List<String> testBeneficiaries = List.of("1", "2", "3", "4");
    private static final JobModel testJobModel = new JobModel(UUID.randomUUID(), UUID.randomUUID(),
            Collections.singletonList(ResourceType.Patient),
            TEST_PROVIDER_ID,
            testBeneficiaries);

    private ResourceExtension groupResource = ResourceExtension.builder()
            .addResource(new GroupResource(queue, client, TEST_BASE_URL))
            .addResource(new JobResource(queue, TEST_BASE_URL))
            .setTestContainerFactory(testContainer)
            .addProvider(staticFilter)
            .addProvider(new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class))
            .build();


    @BeforeAll
    static void setup() {
        mockClient();

        doAnswer(answer -> {
            final JobModel data = answer.getArgument(1);
            assertEquals(testJobModel.getPatients().size(), data.getPatients().size(), "Should have 4 patients");
            return answer.callRealMethod();
        }).when(queue).submitJob(Mockito.any(UUID.class), Mockito.any(JobModel.class));
    }

    @Test
    void testDataRequest() {
        final WebTarget target = groupResource.target("/Group/1/$export");
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Check that the job is in progress
        String jobURL = response.getHeaderString("Content-Location").replace(TEST_BASE_URL, "");
        WebTarget jobTarget = groupResource.target(jobURL);
        Response jobResp = jobTarget.request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.ACCEPTED_202, jobResp.getStatus(), "Job should be in progress");

        // Finish the job and check again
        assertEquals(1, queue.queueSize(), "Should have at least one job in queue");
        final var job = queue.workJob().orElseThrow(() -> new IllegalStateException("Should have a job")).getRight();
        queue.completeJob(job.getJobID(), JobStatus.COMPLETED, job.getJobResults());

        jobTarget = groupResource.target(jobURL);
        jobResp = jobTarget.request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.OK_200, jobResp.getStatus(), "Job should be done");
    }

    /**
     * Test with a resource type in the '_type' query parameter
     *///fails
    @Test
    void testOneResourceSubmission() {
        // A request with parameters ...
        final WebTarget target = groupResource
                .target("/Group/1/$export")
                .queryParam("_type", ResourceType.Patient);
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with Patient and EOB resources
        final var job = queue.workJob();
        assertTrue(job.isPresent());
        final var resources = job.get().getRight().getResourceTypes();
        assertAll(() -> assertEquals(resources.size(), 1),
                () -> assertTrue(resources.contains(ResourceType.Patient)));
    }

    /**
     * Test with a list of resource types in the '_type' query parameter
     */
    @Test
    void testTwoResourceSubmission() {
        // A request with parameters ...
        final WebTarget target = groupResource
                .target("/Group/1/$export")
                .queryParam("_type", String.format("%s,%s", ResourceType.Patient, ResourceType.ExplanationOfBenefit));
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with Patient and EOB resources
        var job = queue.workJob();
        assertTrue(job.isPresent());
        var resources = job.get().getRight().getResourceTypes();
        assertAll(() -> assertEquals(resources.size(), 2),
                () -> assertTrue(resources.contains(ResourceType.Patient)),
                () -> assertTrue(resources.contains(ResourceType.ExplanationOfBenefit)));
    }

    @Test
    void testThreeResourceSubmission() {
        // A request with parameters ...
        final WebTarget target = groupResource
                .target("/Group/1/$export")
                .queryParam("_type", String.format("%s,%s,%s", ResourceType.Patient, ResourceType.ExplanationOfBenefit, ResourceType.Coverage));
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with Patient and EOB resources
        var job = queue.workJob();
        assertTrue(job.isPresent());
        var resources = job.get().getRight().getResourceTypes();
        assertAll(() -> assertEquals(3, resources.size()),
                () -> assertTrue(resources.contains(ResourceType.Patient)),
                () -> assertTrue(resources.contains(ResourceType.Coverage)),
                () -> assertTrue(resources.contains(ResourceType.ExplanationOfBenefit)));
    }

    /**
     * Negative test with a bad type of resource types in the '_type' query parameter
     */
    @Test
    void testBadResourceSubmission() {
        // A request with a bad resource type parameter...
        final WebTarget target = groupResource
                .target("/Group/1/$export")
                .queryParam("_type", "BadResource");
        final Response response = target.request().get();
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus(), "Should have 400 status"));

        // Should yield a queue should have no entries
        assertEquals(queue.queueSize(), 0);
    }

    /**
     * Test with no _type parameter
     */
    @Test
    void testNoResourceSubmission() {
        // A request with no resource type parameters...
        final WebTarget target = groupResource.target("/Group/1/$export");
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with all resource types
        var job = queue.workJob();
        assertTrue(job.isPresent());
        var resources = job.get().getRight().getResourceTypes();
        assertAll(() -> assertEquals(resources.size(), JobModel.validResourceTypes.size()));
    }


    @SuppressWarnings("unchecked")
    private static void mockClient() {

        final IOperation mockOperation = mock(IOperation.class);
        final IOperationUnnamed unnamed = mock(IOperationUnnamed.class);
        final IOperationUntyped untypedOperation = mock(IOperationUntyped.class);
        final IOperationUntypedWithInput inputOp = mock(IOperationUntypedWithInput.class);

        Mockito.when(client.read()).thenReturn(mockRead);
        Mockito.when(mockRead.resource(Group.class)).thenReturn(mockTypedRead);
        Mockito.when(mockTypedRead.withId(Mockito.any(IdType.class))).thenReturn(mockExecutable);
        Mockito.when(mockExecutable.encodedJson()).thenReturn(mockExecutable);
        Mockito.when(mockExecutable.execute()).thenAnswer(answer -> {
//            if (resourceCapture.getValue().equals(Bundle.class)) {
//        } else{
            final Group group = new Group();
            group.setId("test-group-id");
            group.addMember().setEntity(new Reference("Patient/test"));

            return group;
//            }
        });

        // Patient Operation
        Mockito.when(client.operation()).thenReturn(mockOperation);
        Mockito.when(mockOperation.onInstance(Mockito.any())).thenReturn(unnamed);
        Mockito.when(unnamed.named("patients")).thenReturn(untypedOperation);
        Mockito.when(untypedOperation.withNoParameters(Mockito.any())).thenReturn(inputOp);
        Mockito.when(inputOp.returnResourceType(Bundle.class)).thenReturn(inputOp);
        Mockito.when(inputOp.useHttpGet()).thenReturn(inputOp);
        Mockito.when(inputOp.encodedJson()).thenReturn(inputOp);
        Mockito.when(inputOp.execute()).thenAnswer(answer -> {
            final Bundle bundle = new Bundle();

            testBeneficiaries.forEach(id -> {
                final Patient p = new Patient();
                p.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(id);
                bundle.addEntry().setResource(p);
            });

            return bundle;
        });
    }
}
