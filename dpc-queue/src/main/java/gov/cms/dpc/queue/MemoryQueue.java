package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobResult;
import gov.cms.dpc.queue.models.JobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
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
    public synchronized void submitJob(UUID jobID, JobModel job) {
        assert(jobID.equals(job.getJobID()) && job.getStatus() == JobStatus.QUEUED);
        logger.debug("Submitting job: {}", jobID);
        this.queue.put(jobID, job);
    }

    @Override
    public synchronized Optional<JobModel> getJob(UUID jobID) {
        final JobModel jobData = this.queue.get(jobID);
        if (jobData == null) {
            return Optional.empty();
        }
        return Optional.of(jobData);
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
            final JobModel job = first.get().getValue();
            job.setRunningStatus();
            this.queue.replace(key, job);

            final var queueDuration = Duration.between(job.getSubmitTime().orElseThrow(), job.getStartTime().orElseThrow());
            logger.debug("Starting to work job {}. {} seconds of queue time.", key, queueDuration.toMillis() / 1000.0);

            return Optional.of(new Pair<>(key, job));
        }
        return Optional.empty();
    }

    @Override
    public synchronized void completeJob(UUID jobID, JobStatus status, List<JobResult> jobResults) {
        assert(status == JobStatus.COMPLETED || status == JobStatus.FAILED);
        final JobModel job = this.queue.get(jobID);
        if (job == null) {
            throw new JobQueueFailure(jobID, "Job does not exist in queue");
        }
        job.setFinishedStatus(status, jobResults);
        this.queue.replace(jobID, job);

        final var workDuration = Duration.between(job.getStartTime().orElseThrow(), job.getCompleteTime().orElseThrow());
        logger.debug("Completed job {} with status {} and duration {} seconds", jobID, status, workDuration.toMillis() / 1000.0);
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

    @Override
    public void assertHealthy() {
        // Memory is always healthy
    }
}
