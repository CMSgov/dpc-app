package gov.cms.dpc.aggregation.qclient;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.models.JobModel;

import java.util.Optional;
import java.util.UUID;

public abstract class AbstractMockQueueClient implements JobQueue {
    @Override
    public void submitJob(UUID jobId, JobModel data) {
        // TODO

    }

    public Optional<JobStatus> getJobStatus(UUID jobId) {
        // TODO
        return Optional.of(JobStatus.COMPLETED);
    }

    @Override
    public Optional<JobModel> getJob(UUID jobID) {
        return Optional.empty();
    }

    @Override
    public void completeJob(UUID uuid, JobStatus jobStatus) {
        // TODO
    }

    public void removeJob(UUID uuid) {
        // TODO
    }

    @Override
    public long queueSize() {
        // TODO
        return -1;
    }
}
