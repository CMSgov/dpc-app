package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

/**
 * Implements a distributed {@link JobQueue} using Redis and Postgres
 */
public class DistributedQueue implements JobQueue {

    private static final Logger logger = LoggerFactory.getLogger(DistributedQueue.class);

    private final Queue<UUID> queue;

    @Inject
    DistributedQueue(RedissonClient client) {
        queue = client.getQueue("jobqueue");
    }

    @Override
    public <T> void submitJob(UUID jobID, T data) {
        logger.debug("Adding job {} to the queue with data {}.", jobID, data);
        // Persist the job in postgres
        // Add to the redis queue
        // Offer?
        boolean added;
        try {
            added = this.queue.add(jobID);
        } catch (RuntimeException e) {

            throw new JobQueueFailure(jobID, e);
        }

        if (!added) {
            logger.error("Job {} not submitted to queue.", jobID);
            throw new JobQueueFailure(jobID, "Unable to add to queue.");
        }
    }

    @Override
    public Optional<JobStatus> getJobStatus(UUID jobID) {
        // Get from Postgres
        return Optional.empty();
    }

    @Override
    public <T> Optional<Pair<UUID, T>> workJob() {
        final UUID jobID = this.queue.poll();
        if (jobID == null) {
            return Optional.empty();
        }

        // Fetch the Job from Postgres
        return Optional.empty();
    }

    @Override
    public void completeJob(UUID jobID, JobStatus status) {
        // Update
    }

    @Override
    public void removeJob(UUID jobID) {
        // Remove from postgres and queue
        this.queue.remove(jobID);
    }

    @Override
    public int queueSize() {
        return this.queue.size();
    }
}
