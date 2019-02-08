package gov.cms.dpc.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple in-memory queue for tracking job statuses
 */
public class MemoryQueue implements JobQueue {

    private static Logger logger = LoggerFactory.getLogger(MemoryQueue.class);

    private final Map<UUID, JobStatus> queue;

    public MemoryQueue() {
        this.queue = new HashMap<>();
    }

    @Override
    public synchronized void submitJob(UUID jobID) {
        logger.debug("Submitting job: {}", jobID);
        this.queue.put(jobID, JobStatus.QUEUED);
    }

    @Override
    public synchronized Optional<JobStatus> getJobStatus(UUID jobID) {
        logger.debug("Getting status for job: {}", jobID);
        return Optional.ofNullable(this.queue.get(jobID));

    }

    @Override
    public synchronized Optional<UUID> workJob() {
        logger.debug("Pulling first QUEUED job");
        final Optional<Map.Entry<UUID, JobStatus>> first = this.queue.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().equals(JobStatus.QUEUED))
                .findFirst();

        if (first.isPresent()) {
            final UUID key = first.get().getKey();
            logger.debug("Found job {}", key);
            this.queue.replace(key, JobStatus.RUNNING);
            return Optional.of(key);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void completeJob(UUID jobID, JobStatus status) {
        logger.debug("Completed job {} with status: {}", jobID, status);
        this.queue.replace(jobID, status);
    }
}
