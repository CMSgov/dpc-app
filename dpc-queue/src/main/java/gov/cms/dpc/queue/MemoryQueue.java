package gov.cms.dpc.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple in-memory queue for tracking job statuses
 */
public class MemoryQueue implements JobQueue {

    private static Logger logger = LoggerFactory.getLogger(MemoryQueue.class);

    private final Map<UUID, JobModel<Object>> queue;

    @Inject
    public MemoryQueue() {
        this.queue = new HashMap<>();
    }

    @Override
    public synchronized <T> void submitJob(UUID jobID, T data) {
        logger.debug("Submitting job: {}", jobID);
        this.queue.put(jobID, new JobModel<>(JobStatus.QUEUED, data));
    }

    @Override
    public synchronized Optional<JobStatus> getJobStatus(UUID jobID) {
        logger.debug("Getting status for job: {}", jobID);
        final JobModel<Object> jobData = this.queue.get(jobID);
        if (jobData == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.queue.get(jobID).getStatus());

    }

    @Override
    public synchronized <T> Optional<Pair<UUID, T>> workJob() {
        logger.debug("Pulling first QUEUED job");
        final Optional<Map.Entry<UUID, JobModel<Object>>> first = this.queue.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().getStatus().equals(JobStatus.QUEUED))
                .findFirst();

        if (first.isPresent()) {
            final UUID key = first.get().getKey();
            final JobModel<Object> data = first.get().getValue();
            data.setStatus(JobStatus.RUNNING);
            logger.debug("Found job {}", key);
            this.queue.replace(key, data);
            // FIXME(nickrobison): Get rid of this unsafe cast
            return Optional.of(new Pair<>(key, (T) data.getData()));
        }
        return Optional.empty();
    }

    @Override
    public synchronized void completeJob(UUID jobID, JobStatus status) {
        logger.debug("Completed job {} with status: {}", jobID, status);
        final JobModel<Object> job = this.queue.get(jobID);
        if (job == null) {
            throw new IllegalArgumentException(String.format("Job %s does not exist in queue", jobID));
        }

        job.setStatus(status);
        this.queue.replace(jobID, job);
    }

    @Override
    public synchronized void removeJob(UUID jobID) {
        this.queue.remove(jobID);
    }

    @Override
    public int queueSize() {
        return this.queue.size();
    }
}
