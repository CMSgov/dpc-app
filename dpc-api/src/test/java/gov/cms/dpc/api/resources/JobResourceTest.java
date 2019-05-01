package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.api.resources.v1.JobResource;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

public class JobResourceTest {
    static final String TEST_PROVIDER_ID = "1";
    static final String TEST_PATIENT_ID = "1";
    static final String TEST_BASEURL = "http://localhost:8080";

    /**
     * Test that a non-existent job is handled correctly
     */
    @Test
    public void testNonExistentJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryQueue();
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(jobID.toString());

        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
    }

    /**
     * Test with a queued job
     */
    @Test
    public void testQueuedJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryQueue();

        // Setup a queued job
        final var job = new JobModel(jobID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals(JobStatus.QUEUED.toString(), response.getHeaderString("X-Progress")));
    }

    /**
     * Test with a running job
     */
    @Test
    public void testRunningJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryQueue();

        // Setup a running job
        final var job = new JobModel(jobID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.ACCEPTED_202, response.getStatus()),
                () -> assertEquals(JobStatus.RUNNING.toString(), response.getHeaderString("X-Progress")));
    }

    /**
     * Test with a successful job
     */
    @Test
    public void testSuccessfulJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryQueue();

        // Setup a completed job
        final var job = new JobModel(jobID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();
        queue.completeJob(jobID, JobStatus.COMPLETED, List.of());

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.OK_200, response.getStatus()));

        // Test the completion model
        final var completion = (JobCompletionModel) response.getEntity();
        for (JobCompletionModel.OutputEntry entry: completion.getOutput()) {
            assertTrue(JobModel.validResourceTypes.contains(entry.getType()), "Invalid resource type");
            assertEquals(String.format("%s/Data/%s", TEST_BASEURL, JobModel.outputFileName(jobID, entry.getType())), entry.getUrl());
        }
    }

    /**
     * Test with a failed job
     */
    @Test
    public void testFailedJob() {
        final var jobID = UUID.randomUUID();
        final var queue = new MemoryQueue();

        // Setup a failed job
        final var job = new JobModel(jobID,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                List.of(TEST_PATIENT_ID));
        queue.submitJob(jobID, job);
        queue.workJob();
        queue.completeJob(jobID, JobStatus.FAILED, List.of());

        // Test the response
        final var resource = new JobResource(queue, TEST_BASEURL);
        final Response response = resource.checkJobStatus(jobID.toString());
        assertAll(() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus()));
    }
}