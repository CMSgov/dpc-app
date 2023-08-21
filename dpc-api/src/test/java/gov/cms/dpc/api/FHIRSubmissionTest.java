package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFilter;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.api.resources.v1.GroupResource;
import gov.cms.dpc.api.resources.v1.JobResource;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.parameters.ProvenanceResourceFactoryProvider;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_HEADER;
import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_RESPOND_ASYNC;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that a user can successfully submit a data export job
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@SuppressWarnings("rawtypes")
class FHIRSubmissionTest {
    private static final String TEST_BASE_URL = "http://localhost:3002/v1";
    private static final UUID AGGREGATOR_ID = UUID.randomUUID();
    private static final IJobQueue queue = spy(MemoryBatchQueue.class);
    private static final IGenericClient client = mock(IGenericClient.class);
    private static final BlueButtonClient bfdClient = mock(BlueButtonClient.class);
    private static final IRead mockRead = mock(IRead.class);
    private static final IReadTyped mockTypedRead = mock(IReadTyped.class);
    private static final IReadExecutable mockExecutable = mock(IReadExecutable.class);
    private static final ProvenanceResourceFactoryProvider factory = mock(ProvenanceResourceFactoryProvider.class);

    private static final AuthFilter<DPCAuthCredentials, OrganizationPrincipal> staticFilter = new StaticAuthFilter(new StaticAuthenticator());
    private static final GrizzlyWebTestContainerFactory testContainer = new GrizzlyWebTestContainerFactory();

    // Test data
    private static final List<String> testBeneficiaries = List.of("0Z00Z00ZZ01", "0Z00Z00ZZ02", "0Z00Z00ZZ03", "0Z00Z00ZZ04");

    private final ResourceExtension groupResource = ResourceExtension.builder()
            .addResource(new GroupResource(queue, client, TEST_BASE_URL, bfdClient, new DPCAPIConfiguration()))
            .addResource(new JobResource(queue, TEST_BASE_URL))
            .setTestContainerFactory(testContainer)
            .addProvider(staticFilter)
            .addProvider(new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class))
            .addProvider(factory)
            .build();

    @BeforeAll
    static void setup() {
        mockFactory();
        mockClient();
        mockBfdClient();
        doCallRealMethod().when(queue).createJob(Mockito.any(UUID.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyList(), Mockito.any(OffsetDateTime.class), Mockito.any(OffsetDateTime.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    void testDataRequest() {
        final WebTarget target = groupResource.target("/v1/Group/1/$export");
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Check that the job is in progress
        String jobURL = response.getHeaderString("Content-Location").replace("http://localhost:3002/", "");
        WebTarget jobTarget = groupResource.target(jobURL);
        Response jobResp = jobTarget.request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.ACCEPTED_202, jobResp.getStatus(), "Job should be in progress");

        // Finish the job and check again
        assertEquals(1, queue.queueSize(), "Should have at least one job in queue");
        final var job = queue.claimBatch(AGGREGATOR_ID).orElseThrow(() -> new IllegalStateException("Should have a job"));
        while (job.fetchNextPatient(AGGREGATOR_ID).isPresent()) {
            queue.completePartialBatch(job, AGGREGATOR_ID);
        }
        queue.completeBatch(job, AGGREGATOR_ID);

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
                .target("/v1/Group/1/$export")
                .queryParam("_type", DPCResourceType.Patient);
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with Patient and EOB resources
        final var job = queue.claimBatch(AGGREGATOR_ID);
        assertTrue(job.isPresent());
        final var resources = job.get().getResourceTypes();
        assertAll(() -> assertEquals(resources.size(), 1),
                () -> assertTrue(resources.contains(DPCResourceType.Patient)));
    }

    /**
     * Test with a list of resource types in the '_type' query parameter
     */
    @Test
    void testTwoResourceSubmission() {
        // A request with parameters ...
        final WebTarget target = groupResource
                .target("/v1/Group/1/$export")
                .queryParam("_type", String.format("%s,%s", DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit));
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with Patient and EOB resources
        var job = queue.claimBatch(AGGREGATOR_ID);
        assertTrue(job.isPresent());
        var resources = job.get().getResourceTypes();
        assertAll(() -> assertEquals(2, resources.size()),
                () -> assertTrue(resources.contains(DPCResourceType.Patient)),
                () -> assertTrue(resources.contains(DPCResourceType.ExplanationOfBenefit)));
    }

    @Test
    void testThreeResourceSubmission() {
        // A request with parameters ...
        final WebTarget target = groupResource
                .target("/v1/Group/1/$export")
                .queryParam("_type", String.format("%s,%s,%s", DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit, DPCResourceType.Coverage));
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with Patient and EOB resources
        var job = queue.claimBatch(AGGREGATOR_ID);
        assertTrue(job.isPresent());
        var resources = job.get().getResourceTypes();
        assertAll(() -> assertEquals(3, resources.size()),
                () -> assertTrue(resources.contains(DPCResourceType.Patient)),
                () -> assertTrue(resources.contains(DPCResourceType.Coverage)),
                () -> assertTrue(resources.contains(DPCResourceType.ExplanationOfBenefit)));
    }

    /**
     * Negative test with a bad type of resource types in the '_type' query parameter
     */
    @Test
    void testBadResourceSubmission() {
        // A request with a bad resource type parameter...
        final WebTarget target = groupResource
                .target("/v1/Group/1/$export")
                .queryParam("_type", "BadResource");
        final Response response = target.request().get();
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus(), "Should have 400 status"));

        // Should yield a queue should have no entries
        assertEquals(0, queue.queueSize());
    }

    /**
     * Test with no _type parameter
     */
    @Test
    void testNoResourceSubmission() {
        // A request with no resource type parameters...
        final WebTarget target = groupResource.target("/v1/Group/1/$export");
        final Response response = target.request()
                .accept(FHIR_JSON).header(PREFER_HEADER, PREFER_RESPOND_ASYNC)
                .get();
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus(), "Should have 202 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));

        // Should yield a job with all resource types
        var job = queue.claimBatch(AGGREGATOR_ID);
        assertTrue(job.isPresent());
        var resources = job.get().getResourceTypes();
        assertAll(() -> assertEquals(resources.size(), JobQueueBatch.validResourceTypes.size()));
    }

    /**
     * The BFD is used to return a mock lastUpdate for the BFD service
     */
    private static void mockBfdClient() {
        Bundle mockBundle = new Bundle();
        mockBundle.getMeta().setLastUpdated(new Date());
        Mockito.when(bfdClient.requestPatientFromServer(anyString(), isNull(), any())).thenReturn(mockBundle);
    }

    @SuppressWarnings("unchecked")
    private static void mockClient() {

        final IOperation mockOperation = mock(IOperation.class);
        final IOperationUnnamed unnamed = mock(IOperationUnnamed.class);
        final IOperationUntyped untypedOperation = mock(IOperationUntyped.class);
        final IOperationUntypedWithInput inputOp = mock(IOperationUntypedWithInput.class);
        final IOperationUntypedWithInputAndPartialOutput<IBaseParameters> paramOp = mock(IOperationUntypedWithInputAndPartialOutput.class);

        Mockito.when(client.read()).thenReturn(mockRead);
        Mockito.when(mockRead.resource(Group.class)).thenAnswer(answer -> {
            mockReadResource(Group.class);
            return mockTypedRead;
        });
        Mockito.when(mockRead.resource(Organization.class)).thenAnswer(answer -> {
            mockReadResource(Organization.class);
            return mockTypedRead;
        });

        // Patient Operation
        Mockito.when(client.operation()).thenReturn(mockOperation);
        Mockito.when(mockOperation.onInstance(Mockito.any())).thenReturn(unnamed);
        Mockito.when(unnamed.named("patients")).thenReturn(untypedOperation);
        Mockito.when(untypedOperation.withParameters(Mockito.any())).thenReturn(paramOp);
        Mockito.when(paramOp.returnResourceType(Bundle.class)).thenReturn(inputOp);
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

    private static void mockReadResource(Class clazz) {
        Mockito.when(mockTypedRead.withId(Mockito.any(IdType.class))).thenReturn(mockExecutable);
        Mockito.when(mockExecutable.encodedJson()).thenReturn(mockExecutable);
        Mockito.when(mockExecutable.execute()).thenAnswer(answer -> {
            if (clazz.equals(Organization.class)) {
                Organization organization = new Organization();
                UUID orgId = UUID.randomUUID();
                organization.setId(orgId.toString());
                Identifier identifier = new Identifier();
                identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(NPIUtil.generateNPI());
                organization.setIdentifier(List.of(identifier));
                return organization;
            } else {
                final Group group = new Group();
                group.setId("test-group-id");
                group.addMember().setEntity(new Reference("Patient/test"));
                group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
                CodeableConcept codeableConcept = new CodeableConcept();
                codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
                group.getCharacteristicFirstRep().setValue(codeableConcept);
                return group;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void mockFactory() {
        Mockito.when(factory.getPriority()).thenReturn(ValueParamProvider.Priority.NORMAL);
        final Function<ContainerRequest, java.util.function.Supplier<?>> func = mock(Function.class);
        Mockito.when(factory.getValueProvider(Mockito.any())).thenReturn(func);
    }
}
