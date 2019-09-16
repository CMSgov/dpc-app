package gov.cms.dpc.queue;

import gov.cms.dpc.queue.models.JobQueueBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple in-memory queue for tracking job statuses
 */
public class MemoryBatchQueue extends JobQueueCommon {

    private static Logger logger = LoggerFactory.getLogger(MemoryBatchQueue.class);

    private final Map<UUID, JobQueueBatch> queue;

    public MemoryBatchQueue(int batchSize) {
        super(batchSize);
        this.queue = new HashMap<>();
    }

    @Override
    protected synchronized void submitJobBatches(List<JobQueueBatch> jobBatches) {
        jobBatches.forEach(batch -> {
            logger.debug("Submitting batch {}", batch.getBatchID());
            this.queue.put(batch.getBatchID(), batch);
            batch.setUpdateTime();
        });
    }

    @Override
    public synchronized Optional<JobQueueBatch> getBatch(UUID batchID) {
        return this.queue.containsKey(batchID) ?
                Optional.of(this.queue.get(batchID)) :
                Optional.empty();
    }

    @Override
    public synchronized List<JobQueueBatch> getJobBatches(UUID jobID) {
        return this.queue.values().stream()
                .filter(batch -> batch.getJobID().equals(jobID))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Optional<JobQueueBatch> workBatch(UUID aggregatorID) {
        logger.debug("Pulling first QUEUED job");
        final Optional<JobQueueBatch> first = this.queue.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getStatus().equals(JobStatus.QUEUED))
                .map(Map.Entry::getValue)
                .findFirst();

        if ( first.isPresent() ) {
            first.get().setRunningStatus(aggregatorID);
        }

        return first;
    }

    @Override
    public synchronized void pauseBatch(JobQueueBatch job, UUID aggregatorID) {
        job.setPausedStatus(aggregatorID);
        job.setUpdateTime();
    }

    @Override
    public synchronized void completePartialBatch(JobQueueBatch job, UUID aggregatorID) {
        job.setUpdateTime();
    }

    @Override
    public synchronized void completeBatch(JobQueueBatch job, UUID aggregatorID) {
        job.setCompletedStatus(aggregatorID);
        job.setUpdateTime();
    }

    @Override
    public synchronized void failBatch(JobQueueBatch job, UUID aggregatorID) {
        job.setFailedStatus(aggregatorID);
        job.setUpdateTime();
    }

    @Override
    public synchronized long queueSize() {
        return this.queue.values().stream()
                .filter(queue -> queue.getStatus().equals(JobStatus.QUEUED))
                .count();
    }

    @Override
    public String queueType() {
        return "MemoryBatchQueue";
    }

    @Override
    public void assertHealthy(UUID aggregatorID) {
        // Memory is always healthy
    }
}
