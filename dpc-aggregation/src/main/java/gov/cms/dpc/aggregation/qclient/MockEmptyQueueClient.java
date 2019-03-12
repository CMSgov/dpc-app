package gov.cms.dpc.aggregation.qclient;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;

import java.util.Optional;
import java.util.UUID;

public class MockEmptyQueueClient implements JobQueue {
    public <T> void submitJob(UUID jobId, T data) {
        // TODO

    }

    public Optional<JobStatus> getJobStatus(UUID jobId) {
        // TODO
        return Optional.of(JobStatus.COMPLETED);
    }

    public <T> Optional<Pair<UUID, T>> workJob() {
        return Optional.empty();
    }

    public void completeJob(UUID uuid, JobStatus jobStatus) {
        // TODO
    }

    public void removeJob(UUID uuid) {
        // TODO
    }

    public int queueSize() {
        // TODO
        return -1;
    }
}
