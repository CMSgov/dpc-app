package gov.cms.dpc.api.resources.r4;

import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.stu3.FHIRExtractors;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusOutput;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusResponse;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(BufferedLoggerHandler.class)
public class JobResourceTest {

    static final String TEST_ORG_NPI = NPIUtil.generateNPI();
    static final String TEST_PROVIDER_NPI = NPIUtil.generateNPI();
    static final String TEST_PATIENT_MBI = "9S79A00AA00";
    static final String TEST_BASEURL = "http://localhost:8080/v3";
    static final String TEST_JOB_URL = TEST_BASEURL + "/Group/%s/$export";
    static final String OTHER_ORGANIZATION = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a1";

    @Mock
    OneEcosystemClient client;

    private JobQueueBatch buildJobQueueBatch(UUID orgID, UUID groupID, String requestUrl) {
        return new JobQueueBatch.Builder()
                .jobID(UUID.randomUUID())
                .orgID(orgID)
                .orgNPI(TEST_ORG_NPI)
                .providerNPI(TEST_PROVIDER_NPI)
                .patients(List.of(TEST_PATIENT_MBI))
                .resourceTypes(JobQueueBatch.validResourceTypes)
                .requestUrl(requestUrl)
                .groupID(groupID)
                .isBulk(true)
                .build();
    }

    private UUID createAndSubmitExternalJob(IJobQueue queue, UUID orgID) {
        final UUID groupID = UUID.randomUUID();
        final JobQueueBatch batch = buildJobQueueBatch(orgID, groupID, String.format(TEST_JOB_URL, groupID));
        return queue.createExternalJob(batch, false);
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNonExistentJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryBatchQueue(100);
        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();

        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());

        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
    }

    @Test
    void testInProgressJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);

        final var jobID = createAndSubmitExternalJob(queue, orgID);

        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(202, null));

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());

        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals("QUEUED: 0.00%", response.getHeaderString("X-Progress")));
    }

    @Test
    void testSuccessfulJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();
        final var requestUrl = String.format(TEST_JOB_URL, groupID);

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgID, groupID, requestUrl), false);

        final Instant transactionTime = Instant.now();
        final List<OneEcosystemJobStatusOutput> outputs = JobQueueBatch.validResourceTypes.stream()
                .map(resourceType -> new OneEcosystemJobStatusOutput(
                        resourceType.name(),
                        "/data/" + jobID + "/" + jobID + "-" + resourceType.name() + ".ndjson"))
                .toList();

        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(200,
                new OneEcosystemJobStatusResponse(transactionTime, true, outputs, List.of(), jobID)));

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());

        assertEquals(HttpStatus.OK_200, response.getStatus());

        final var completion = (JobCompletionModel) response.getEntity();
        assertAll(() -> assertEquals(JobQueueBatch.validResourceTypes.size(), completion.getOutput().size()),
                () -> assertEquals(0, completion.getError().size()),
                () -> assertEquals(requestUrl, completion.getRequest()),
                () -> assertEquals(OffsetDateTime.ofInstant(transactionTime, ZoneOffset.UTC), completion.getTransactionTime()));

        for (JobCompletionModel.OutputEntry entry : completion.getOutput()) {
            assertEquals(TEST_BASEURL + "/Data/" + jobID + "/" + jobID + "-" + entry.getType().name() + ".ndjson", entry.getUrl());
        }
    }

    @Test
    void testJobWithError() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgID, groupID, String.format(TEST_JOB_URL, groupID)),  false);

        final List<String> partialErrors = List.of(
                "Failed to retrieve ExplanationOfBenefit data for 3 of 150 patients: BFD returned 504 Gateway Timeout");

        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(200,
                new OneEcosystemJobStatusResponse(Instant.now(), true, List.of(), partialErrors, jobID)));

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());

        assertEquals(HttpStatus.OK_200, response.getStatus());
        final var completion = (JobCompletionModel) response.getEntity();
        assertAll(() -> assertTrue(completion.getOutput().isEmpty()),
                () -> assertEquals(1, completion.getError().size()),
                () -> assertEquals(DPCResourceType.OperationOutcome, completion.getError().getFirst().getType()),
                () -> assertEquals(partialErrors.size(), completion.getError().getFirst().getCount()),
                () -> assertEquals(TEST_BASEURL + "/Jobs/" + jobID + "/OperationOutcome", completion.getError().getFirst().getUrl()));
    }

    @Test
    void testFailedJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgID, groupID, String.format(TEST_JOB_URL, groupID)), false);
        final var batch = queue.getJobBatches(jobID).getFirst();

        final List<String> fatalErrors = List.of(
                "BCDA job aborted: downstream BFD service was unreachable after 3 retries");

        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(500,
                new OneEcosystemJobStatusResponse(Instant.now(), true, List.of(), fatalErrors, jobID)));

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        String jobString = jobID.toString();
        JobQueueFailure exception = assertThrows(JobQueueFailure.class,
                () -> resource.checkJobStatus(organizationPrincipal, jobString));
        assertEquals(String.format("Operation on Job(%s) Batch(%s) failed for reason: Batch failed", jobID, batch.getBatchID()), exception.getMessage());
    }

    @Test
    void testUnknownJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgID, groupID, String.format(TEST_JOB_URL, groupID)), false);

        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(400,
                new OneEcosystemJobStatusResponse(Instant.now(), true, List.of(),
                        List.of("No job found with ID " + jobID), jobID)));

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());

        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
    }

    @Test
    void testUnrecognizedBackendStatus() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgID, groupID, String.format(TEST_JOB_URL, groupID)),  false);
        final var batch = queue.getJobBatches(jobID).getFirst();

        OneEcosystemJobStatus status = new OneEcosystemJobStatus(503,
                new OneEcosystemJobStatusResponse(Instant.now(), true, List.of(), List.of(), jobID));
        when(client.jobStatus(jobID)).thenReturn(status);

        final var resource = new JobResource(queue, client, TEST_BASEURL);

        String jobString = jobID.toString();
        JobQueueFailure exception = assertThrows(JobQueueFailure.class,
                () -> resource.checkJobStatus(organizationPrincipal, jobString));
        assertEquals(String.format("Operation on Job(%s) Batch(%s) failed for reason: Export job returned unrecognized status code: %s", jobID, batch.getBatchID(), status.statusCode()), exception.getMessage());
    }

    @Test
    void testWrongOrgJobAccess() {
        final var organizationPrincipalCorrect = APITestHelpers.makeOrganizationPrincipal();
        final var orgIDCorrect = FHIRExtractors.getEntityUUID(organizationPrincipalCorrect.getOrganization().getId());
        final var organizationPrincipalWrong = APITestHelpers.makeOrganizationPrincipal(OTHER_ORGANIZATION);
        final var queue = new MemoryBatchQueue(100);

        final var jobID = createAndSubmitExternalJob(queue, orgIDCorrect);

        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(200,
                new OneEcosystemJobStatusResponse(Instant.now(), true, List.of(), List.of(), jobID)));

        final var resource = new JobResource(queue, client, TEST_BASEURL);

        final Response responseWrong = resource.checkJobStatus(organizationPrincipalWrong, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, responseWrong.getStatus()),
                () -> assertEquals("Invalid organization for job", responseWrong.getEntity()));

        final Response responseRight = resource.checkJobStatus(organizationPrincipalCorrect, jobID.toString());
        assertEquals(HttpStatus.OK_200, responseRight.getStatus());
    }

    @Test
    void testGetJobErrorsReturnsOperationOutcome() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgID, groupID, String.format(TEST_JOB_URL, groupID)), false);

        final List<String> errors = List.of("BFD returned 504 Gateway Timeout for 3 of 150 patients");
        when(client.jobStatus(jobID)).thenReturn(new OneEcosystemJobStatus(200,
                new OneEcosystemJobStatusResponse(Instant.now(), true, List.of(), errors, jobID)));

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final Response response = resource.getJobErrors(organizationPrincipal, jobID.toString());

        assertEquals(HttpStatus.OK_200, response.getStatus());
        final OperationOutcome outcome = (OperationOutcome) response.getEntity();
        assertAll(() -> assertEquals(1, outcome.getIssue().size()),
                () -> assertEquals(errors.getFirst(), outcome.getIssueFirstRep().getDiagnostics()),
                () -> assertEquals(OperationOutcome.IssueSeverity.ERROR, outcome.getIssueFirstRep().getSeverity()));
    }

    @Test
    void testGetJobErrorsWrongOrganization() {
        final var organizationPrincipalCorrect = APITestHelpers.makeOrganizationPrincipal();
        final var orgIDCorrect = FHIRExtractors.getEntityUUID(organizationPrincipalCorrect.getOrganization().getId());
        final var organizationPrincipalWrong = APITestHelpers.makeOrganizationPrincipal(OTHER_ORGANIZATION);
        final var queue = new MemoryBatchQueue(100);
        final var groupID = UUID.randomUUID();

        final var jobID = queue.createExternalJob(buildJobQueueBatch(orgIDCorrect, groupID, String.format(TEST_JOB_URL, groupID)), false);

        final var resource = new JobResource(queue, client, TEST_BASEURL);
        final Response response = resource.getJobErrors(organizationPrincipalWrong, jobID.toString());

        assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    void testGetJobErrorsNonExistentJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var queue = new MemoryBatchQueue(100);
        final var resource = new JobResource(queue, client, TEST_BASEURL);

        final Response response = resource.getJobErrors(organizationPrincipal, UUID.randomUUID().toString());

        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
    }
}
