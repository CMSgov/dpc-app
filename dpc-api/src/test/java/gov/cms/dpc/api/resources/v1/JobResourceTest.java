package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
public class JobResourceTest {
    static final UUID AGGREGATOR_ID = UUID.randomUUID();
    static final String TEST_ORG_NPI = NPIUtil.generateNPI();
    static final String TEST_PROVIDER_NPI = NPIUtil.generateNPI();
    static final String TEST_PATIENT_ID = UUID.randomUUID().toString();
    static final String TEST_BASEURL = "http://localhost:8080";
    static final String TEST_JOB_URL = TEST_BASEURL + "/api/v1/Group/%s/$export";
    static final String OTHER_ORGANIZATION = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a1";

    /**
     * Test that a non-existent job is handled correctly
     */
    @Test
    public void testNonExistentJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryBatchQueue(100);
        final var resource = new JobResource(queue, TEST_BASEURL);
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();

        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());

        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
    }

    /**
     * Test with a queued job
     */
    @Test
    public void testQueuedJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);

        // Setup a queued job
        final var jobID = queue.createJob(orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, true);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals("QUEUED: 0.00%", response.getHeaderString("X-Progress")));
    }

    /**
     * Test with a running job
     */
    @Test
    public void testRunningJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(1);

        // Setup a running job
        final var jobID = queue.createJob(orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID, TEST_PATIENT_ID, TEST_PATIENT_ID),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, true);
        final var runningJob = queue.claimBatch(AGGREGATOR_ID);
        runningJob.flatMap(job -> job.fetchNextPatient(AGGREGATOR_ID));
        queue.completePartialBatch(runningJob.get(), AGGREGATOR_ID);
        queue.completeBatch(runningJob.get(), AGGREGATOR_ID);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals("RUNNING: 33.33%", response.getHeaderString("X-Progress")));
    }

    /**
     * Test with a successful job
     */
    @Test
    public void testSuccessfulJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);

        // Setup a completed job
        final var requestUrl = String.format(TEST_JOB_URL, UUID.randomUUID().toString());
        final var jobID = queue.createJob(orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null,
                requestUrl, true);
        queue.claimBatch(AGGREGATOR_ID);

        final var runningJob = queue.getJobBatches(jobID).get(0);
        runningJob.fetchNextPatient(AGGREGATOR_ID);
        final var results = JobQueueBatch.validResourceTypes.stream()
                .map(resourceType -> runningJob.addJobQueueFile(resourceType, 0, 1))
                .collect(Collectors.toList());

        queue.completeBatch(runningJob, AGGREGATOR_ID);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, response.getStatus()));

        var expires = ZonedDateTime.parse(response.getHeaderString("Expires"), JobResource.HTTP_DATE_FORMAT);
        assertAll(() -> assertTrue(expires.isAfter(ZonedDateTime.now().plusHours(23))),
                () -> assertTrue(expires.isBefore(ZonedDateTime.now().plusHours(25))));

        // Test the completion model
        final var completion = (JobCompletionModel) response.getEntity();
        assertAll(() -> assertEquals(JobQueueBatch.validResourceTypes.size(), completion.getOutput().size()),
                () -> assertEquals(0, completion.getError().size()));
        assertEquals(completion.getRequest(), requestUrl);
        for (JobCompletionModel.OutputEntry entry : completion.getOutput()) {
            assertEquals(String.format("%s/Data/%s.ndjson", TEST_BASEURL, JobQueueBatchFile.formOutputFileName(runningJob.getBatchID(), entry.getType(), 0)), entry.getUrl());
        }
    }


    /**
     * Test with a successful job with one patient error
     */
    @Test
    public void testJobWithError() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);

        // Setup a completed job with one error
        final var requestUrl = String.format(TEST_JOB_URL, UUID.randomUUID().toString()) + "?since=2020-02-20T12:00:00.000-05:00";
        final var jobID = queue.createJob(orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, requestUrl, true);
        queue.claimBatch(AGGREGATOR_ID);

        final var runningJob = queue.getJobBatches(jobID).get(0);
        runningJob.fetchNextPatient(AGGREGATOR_ID);
        runningJob.addJobQueueFile(DPCResourceType.OperationOutcome, 0, 1);

        queue.completeBatch(runningJob, AGGREGATOR_ID);

        // Test the response for ok
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, response.getStatus()));

        // Test the completion model
        final var completion = (JobCompletionModel) response.getEntity();
        assertAll(() -> assertEquals(0, completion.getOutput().size()),
                () -> assertEquals(1, completion.getError().size()));
        assertEquals(completion.getRequest(), requestUrl);
        JobCompletionModel.OutputEntry entry = completion.getError().get(0);
        assertEquals(DPCResourceType.OperationOutcome, entry.getType());
        assertEquals(String.format("%s/Data/%s.ndjson", TEST_BASEURL, JobQueueBatchFile.formOutputFileName(runningJob.getBatchID(), DPCResourceType.OperationOutcome, 0)), entry.getUrl());
    }

    /**
     * Test with a failed job
     */
    @Test
    public void testFailedJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(100);

        // Setup a failed job
        final var jobID = queue.createJob(orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, true);
        queue.claimBatch(AGGREGATOR_ID);

        final var runningJob = queue.getJobBatches(jobID).get(0);
        queue.failBatch(runningJob, AGGREGATOR_ID);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus()));
    }

    @Test
    public void testExpiredJob() {
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryBatchQueue(1);

        final UUID jobId = queue.createJob(orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID, "2", "3"),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, true);

        List<JobQueueBatch> batches = queue.getJobBatches(jobId);
        OffsetDateTime timeAgo = OffsetDateTime.now().minusHours(24);
        for (JobQueueBatch batch : batches) {
            queue.claimBatch(AGGREGATOR_ID);
            batch.fetchNextPatient(AGGREGATOR_ID);
            batch.addJobQueueFile(DPCResourceType.OperationOutcome, 0, 1);
            queue.completeBatch(batch, AGGREGATOR_ID);
            timeAgo = timeAgo.minusMinutes(5);
            batch.setCompleteTime(timeAgo);
        }

        final var resource = new JobResource(queue, TEST_BASEURL);
        var response = resource.checkJobStatus(organizationPrincipal, jobId.toString());
        assertEquals(HttpStatus.GONE_410, response.getStatus());

        for (JobQueueBatch batch : batches) {
            batch.setCompleteTime(OffsetDateTime.now().minusHours(23));
        }

        response = resource.checkJobStatus(organizationPrincipal, jobId.toString());
        assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    /**
     * Test accessing a job with the wrong organization
     */
    @Test
    public void testWrongOrgJobAccess() {
        final var organizationPrincipalCorrect = APITestHelpers.makeOrganizationPrincipal();
        final var orgIDCorrect = FHIRExtractors.getEntityUUID(organizationPrincipalCorrect.getOrganization().getId());
        final var organizationPrincipalWrong = APITestHelpers.makeOrganizationPrincipal(OTHER_ORGANIZATION);

        final var queue = new MemoryBatchQueue(100);

        // Setup a completed job
        final var jobID = queue.createJob(orgIDCorrect,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(TEST_PATIENT_ID),
                JobQueueBatch.validResourceTypes,
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, true);
        queue.claimBatch(AGGREGATOR_ID);

        final var runningJob = queue.getJobBatches(jobID).get(0);
        runningJob.fetchNextPatient(AGGREGATOR_ID);
        final var results = JobQueueBatch.validResourceTypes.stream()
                .map(resourceType -> runningJob.addJobQueueFile(resourceType, 0, 1))
                .collect(Collectors.toList());

        queue.completeBatch(runningJob, AGGREGATOR_ID);

        // Try accessing it with the wrong org (should be unauthorized)
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response responseWrong = resource.checkJobStatus(organizationPrincipalWrong, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, responseWrong.getStatus()));

        // Access it with the right org (should be authorized)
        final Response responseRight = resource.checkJobStatus(organizationPrincipalCorrect, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, responseRight.getStatus()));

        // Test the completion model
        final var completion = (JobCompletionModel) responseRight.getEntity();
        assertAll(() -> assertEquals(JobQueueBatch.validResourceTypes.size(), completion.getOutput().size()),
                () -> assertEquals(0, completion.getError().size()));
        for (JobCompletionModel.OutputEntry entry : completion.getOutput()) {
            assertEquals(String.format("%s/Data/%s.ndjson", TEST_BASEURL, JobQueueBatchFile.formOutputFileName(runningJob.getBatchID(), entry.getType(), 0)), entry.getUrl());
        }
    }

    /**
     * Test building extension for a file.
     */
    @Test
    public void testBuildOutputEntryExtension() {
        final var resource = new JobResource(null, "");
        final var file = new JobQueueBatchFile(UUID.randomUUID(), UUID.fromString("f1e518f5-4977-47c6-971b-7eeaf1b433e8"), DPCResourceType.Patient, 0, 11);
        file.setChecksum(Hex.decode("9d251cea787379c603af13f90c26a9b2a4fbb1e029793ae0f688c5631cdb6a1b"));
        file.setFileLength(7202L);
        List<JobCompletionModel.FhirExtension> extension = resource.buildOutputEntryExtension(file);
        assertAll(() -> assertEquals(JobCompletionModel.CHECKSUM_URL, extension.get(0).getUrl()),
                () -> assertEquals("sha256:9d251cea787379c603af13f90c26a9b2a4fbb1e029793ae0f688c5631cdb6a1b", extension.get(0).getValueString()),
                () -> assertEquals(JobCompletionModel.FILE_LENGTH_URL, extension.get(1).getUrl()),
                () -> assertEquals(7202L, extension.get(1).getValueDecimal()));
    }

    @Test
    public void testBuildJobExtension() {
        final var resource = new JobResource(null, "");
        final var batch = new JobQueueBatch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                OffsetDateTime.now(), null, null, true);
        final var aggregatorId = UUID.randomUUID();
        batch.setRunningStatus(aggregatorId);
        batch.setCompletedStatus(aggregatorId);
        List<JobCompletionModel.FhirExtension> extension = resource.buildJobExtension(Collections.singletonList(batch));
        assertAll(
                () -> assertEquals(JobCompletionModel.SUBMIT_TIME_URL, extension.get(0).getUrl()),
                () -> assertEquals(batch.getSubmitTime().orElseThrow(), extension.get(0).getValueDateTime()),
                () -> assertEquals(JobCompletionModel.COMPLETE_TIME_URL, extension.get(1).getUrl()),
                () -> assertEquals(batch.getCompleteTime().orElseThrow(), extension.get(1).getValueDateTime()));
    }
}