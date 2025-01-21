package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
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

    public MemoryBatchQueue() {
        this(100);
    }

    public MemoryBatchQueue(int batchSize) {
        super(batchSize);
        this.queue = new HashMap<>();
    }

    @Override
    public synchronized void submitJobBatches(List<JobQueueBatch> jobBatches) {
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
    public synchronized Optional<JobQueueBatchFile> getJobBatchFile(UUID organizationID, String fileID) {
        return this.queue.values().stream()
                .filter(batch -> batch.getOrgID().equals(organizationID))
                .flatMap(batch -> batch.getJobQueueBatchFiles().stream())
                .filter(file -> file.getFileName().equals(fileID))
                .findAny();
    }

    @Override
    public synchronized Optional<JobQueueBatch> claimBatch(UUID aggregatorID) {
        logger.debug("Pulling first QUEUED job");
        final Optional<JobQueueBatch> first = this.queue.values()
                .stream()
                .filter(jobQueueBatch -> jobQueueBatch.getStatus().equals(JobStatus.QUEUED))
                .findFirst();

        if (first.isPresent()) {
            try {
                first.get().setRunningStatus(aggregatorID);
            } catch (Exception e) {
                logger.error("Failed to mark job as running. Marking the job as failed", e);
                first.get().setFailedStatus();
                return Optional.empty();
            }
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
        if (job != null) {
            job.setCompletedStatus(aggregatorID);
        } else {
            throw new JobQueueFailure("Empty job passed");
        }
    }

    @Override
    public synchronized void failBatch(JobQueueBatch job, UUID aggregatorID) {
        job.setFailedStatus();
    }

    @Override
    public synchronized long queueSize() {
        return this.queue.values().stream()
                .filter(batch -> batch.getStatus().equals(JobStatus.QUEUED))
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
