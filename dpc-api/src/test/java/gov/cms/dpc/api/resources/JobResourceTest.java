package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.api.resources.v1.JobResource;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

public class JobResourceTest {
    static final String TEST_PROVIDER_ID = "1";
    static final String TEST_PATIENT_ID = "1";
    static final String TEST_BASEURL = "http://localhost:8080";
    static final String OTHER_ORGANIZATION = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a1";

    /**
     * Test that a non-existent job is handled correctly
     */
    @Test
    public void testNonExistentJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryQueue();
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
        final var jobID = UUID.randomUUID();
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryQueue();

        // Setup a queued job
        final var job = new JobModel(jobID, orgID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals(JobStatus.QUEUED.toString(), response.getHeaderString("X-Progress")));
    }

    /**
     * Test with a running job
     */
    @Test
    public void testRunningJob() {
        final var jobID = UUID.randomUUID();
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryQueue();

        // Setup a running job
        final var job = new JobModel(jobID, orgID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals(JobStatus.RUNNING.toString(), response.getHeaderString("X-Progress")));
    }

    /**
     * Test with a successful job
     */
    @Test
    public void testSuccessfulJob() {
        final var jobID = UUID.randomUUID();
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryQueue();

        // Setup a completed job
        final var job = new JobModel(jobID, orgID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();
        final var results = JobModel.validResourceTypes
                .stream()
                .map(resourceType -> new JobResult(jobID, resourceType, 0, 1))
                .collect(Collectors.toList());
        queue.completeJob(jobID, JobStatus.COMPLETED, results);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, response.getStatus()));

        // Test the completion model
        final var completion = (JobCompletionModel) response.getEntity();
        assertAll(() -> assertEquals(JobModel.validResourceTypes.size(), completion.getOutput().size()),
                () -> assertEquals(0, completion.getError().size()));
        for (JobCompletionModel.OutputEntry entry: completion.getOutput()) {
            assertEquals(String.format("%s/Data/%s", TEST_BASEURL, JobResult.formOutputFileName(jobID, entry.getType(), 0)), entry.getUrl());
        }
    }


    /**
     * Test with a successful job with one patient error
     */
    @Test
    public void testJobWithError() {
        final var jobID = UUID.randomUUID();
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryQueue();

        // Setup a completed job with one error
        final var job = new JobModel(jobID, orgID,
                List.of(ResourceType.Patient),
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();
        queue.completeJob(jobID, JobStatus.COMPLETED, List.of(new JobResult(jobID, ResourceType.OperationOutcome, 0, 1)));

        // Test the response for ok
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, response.getStatus()));

        // Test the completion model
        final var completion = (JobCompletionModel) response.getEntity();
        assertAll(() -> assertEquals(0, completion.getOutput().size()),
                () -> assertEquals(1, completion.getError().size()));
        JobCompletionModel.OutputEntry entry = completion.getError().get(0);
        assertEquals(ResourceType.OperationOutcome, entry.getType());
        assertEquals(String.format("%s/Data/%s", TEST_BASEURL, JobResult.formOutputFileName(jobID, ResourceType.OperationOutcome, 0)), entry.getUrl());
    }

    /**
     * Test with a failed job
     */
    @Test
    public void testFailedJob() {
        final var jobID = UUID.randomUUID();
        final var organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        final var orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final var queue = new MemoryQueue();

        // Setup a failed job
        final var job = new JobModel(jobID, orgID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();
        queue.completeJob(jobID, JobStatus.FAILED, job.getJobResults());

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(organizationPrincipal, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus()));
    }

    /**
     * Test accessing a job with the wrong organization
     */
    @Test
    public void testWrongOrgJobAccess() {
        final var jobID = UUID.randomUUID();
        final var organizationPrincipalCorrect = APITestHelpers.makeOrganizationPrincipal();
        final var orgIDCorrect = FHIRExtractors.getEntityUUID(organizationPrincipalCorrect.getOrganization().getId());
        final var organizationPrincipalWrong = APITestHelpers.makeOrganizationPrincipal(OTHER_ORGANIZATION);

        final var queue = new MemoryQueue();

        // Setup a completed job
        final var job = new JobModel(jobID, orgIDCorrect,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();
        final var results = JobModel.validResourceTypes
                .stream()
                .map(resourceType -> new JobResult(jobID, resourceType, 0, 1))
                .collect(Collectors.toList());
        queue.completeJob(jobID, JobStatus.COMPLETED, results);

        // Try accessing it with the wrong org (should be unauthorized)
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response responseWrong = resource.checkJobStatus(organizationPrincipalWrong, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, responseWrong.getStatus()));

        // Access it with the right org (should be authorized)
        final Response responseRight = resource.checkJobStatus(organizationPrincipalCorrect, jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, responseRight.getStatus()));

        // Test the completion model
        final var completion = (JobCompletionModel) responseRight.getEntity();
        assertAll(() -> assertEquals(JobModel.validResourceTypes.size(), completion.getOutput().size()),
                () -> assertEquals(0, completion.getError().size()));
        for (JobCompletionModel.OutputEntry entry: completion.getOutput()) {
            assertEquals(String.format("%s/Data/%s", TEST_BASEURL, JobResult.formOutputFileName(jobID, entry.getType(), 0)), entry.getUrl());
        }
    }
}