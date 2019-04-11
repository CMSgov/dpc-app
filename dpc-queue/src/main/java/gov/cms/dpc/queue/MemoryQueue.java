package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
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

    private final Map<UUID, JobModel> queue;

    @Inject
    public MemoryQueue() {
        this.queue = new HashMap<>();
    }

    @Override
    public synchronized void submitJob(UUID jobID, JobModel data) {
        logger.debug("Submitting job: {}", jobID);
        this.queue.put(jobID, data);
    }

    @Override
    public synchronized Optional<JobStatus> getJobStatus(UUID jobID) {
        logger.debug("Getting status for job: {}", jobID);
        final JobModel jobData = this.queue.get(jobID);
        if (jobData == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.queue.get(jobID).getStatus());

    }

    @Override
    public synchronized Optional<Pair<UUID, JobModel>> workJob() {
        logger.debug("Pulling first QUEUED job");
        final Optional<Map.Entry<UUID, JobModel>> first = this.queue.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().getStatus().equals(JobStatus.QUEUED))
                .findFirst();

        if (first.isPresent()) {
            final UUID key = first.get().getKey();
            final JobModel data = first.get().getValue();
            data.setStatus(JobStatus.RUNNING);
            logger.debug("Found job {}", key);
            this.queue.replace(key, data);
            return Optional.of(new Pair<>(key, data));
        }
        return Optional.empty();
    }

    @Override
    public synchronized void completeJob(UUID jobID, JobStatus status) {
        logger.debug("Completed job {} with status: {}", jobID, status);
        final JobModel job = this.queue.get(jobID);
        if (job == null) {
            throw new JobQueueFailure(jobID, "Job does not exist in queue");
        }

        job.setStatus(status);
        this.queue.replace(jobID, job);
    }

    @Override
    public long queueSize() {
        return this.queue
                .values()
                .stream()
                .filter(job -> job.getStatus() == JobStatus.QUEUED)
                .count();
    }

    @Override
    public String queueType() {
        return "MemoryQueue";
    }
}
