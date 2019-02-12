package gov.cms.dpc.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class QueueTest {

    private MemoryQueue queue;

    @BeforeEach
    public void setupQueue() {
        queue = new MemoryQueue();
    }

    @Test
    public void testSimpleSubmissionCompletion() {
        // Add a couple of jobs
        final Set<UUID> jobSet = new HashSet<>();
        jobSet.add(UUID.randomUUID());
        jobSet.add(UUID.randomUUID());

        jobSet.forEach((job) -> queue.submitJob(job, new TestJob("test job")));
        assertEquals(2, queue.queueSize(), "Should have 2 jobs");

        // Check the status of the job
        final UUID firstJob = getSetFirst(jobSet);
        final Optional<JobStatus> jobStatus = queue.getJobStatus(firstJob);
        assertAll(() -> assertTrue(jobStatus.isPresent(), "Should have a matching status"),
                () -> assertEquals(JobStatus.QUEUED, jobStatus.get(), "Job should be queue"));

        // Complete the job and check its status

        Optional<Pair<UUID, TestJob>> workJob = queue.workJob();
        assertTrue(workJob.isPresent(), "Should have job to work");
        queue.completeJob(workJob.get().getLeft(), JobStatus.COMPLETED);
        // Remove from the job set so we can track what's been done
        jobSet.remove(workJob.get().getLeft());

        final Optional<JobStatus> updatedStatus = queue.getJobStatus(workJob.get().getLeft());
        assertAll(() -> assertTrue(updatedStatus.isPresent(), "Should have job status"),
                () -> assertEquals(JobStatus.COMPLETED, updatedStatus.get(), "Job should be completed"));

        // Fail the job and check its status
        workJob = queue.workJob();
        assertTrue(workJob.isPresent(), "Should have a 2nd job to work");
        queue.completeJob(workJob.get().getLeft(), JobStatus.FAILED);
        jobSet.remove(workJob.get().getLeft());

        Optional<JobStatus> failedStatus = queue.getJobStatus(workJob.get().getLeft());
        assertAll(() -> assertTrue(failedStatus.isPresent(), "Should have job status"),
                () -> assertEquals(JobStatus.FAILED, failedStatus.get(), "Job should have failed"));

        assertAll(() -> assertTrue(jobSet.isEmpty(), "Should have worked both jobs"),
                () -> assertTrue(queue.workJob().isEmpty(), "Should not have another job to work"));

        // Remove some jobs
        queue.removeJob(workJob.get().getLeft());
        assertEquals(1, queue.queueSize(), "Should only have a single job");
    }

    @Test
    public void testMissingJob() {
        UUID jobID = UUID.randomUUID();

        // Check that things are empty
        assertAll(() -> assertTrue(queue.workJob().isEmpty(), "Should not have job to work"),
                () -> assertEquals(0, queue.queueSize(), "Should have an empty queue"));

        assertTrue(queue.getJobStatus(jobID).isEmpty(), "Should not be able to get missing job status");
        assertThrows(IllegalArgumentException.class, () -> queue.completeJob(jobID, JobStatus.FAILED), "Should error when completing a job which does not exist");
    }

    private static <T> T getSetFirst(Set<T> set) {
        return set.stream().findFirst().orElseThrow(() -> new IllegalStateException("Cannot get first from empty array"));
    }

    private static class TestJob implements Statisable {

        private final String data;
        private JobStatus status;

        TestJob(String data) {
            this.data = data;
            this.status = JobStatus.QUEUED;
        }

        @Override
        public JobStatus getStatus() {
            return this.status;
        }

        @Override
        public void setStatus(JobStatus status) {
            this.status = status;
        }
    }
}
