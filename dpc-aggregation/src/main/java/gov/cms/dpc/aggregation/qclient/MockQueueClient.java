package gov.cms.dpc.aggregation.qclient;


import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;

import java.util.*;

public class MockQueueClient implements JobQueue {
    private final Set<String> testBeneficiaryIds = new HashSet<String>(Arrays.asList("20140000008325", "20140000008326"));

    public <T> void submitJob(UUID jobId, T data) {
        // TODO

    }

    public Optional<JobStatus> getJobStatus(UUID jobId) {
        // TODO
        return Optional.of(JobStatus.COMPLETED);
    }

    public <T> Optional<Pair<UUID, T>> workJob() {
        return Optional.of(new Pair<>(
                UUID.randomUUID(),
                (T) new JobModel("testProviderId", testBeneficiaryIds)
        ));
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
