package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements a distributed {@link JobQueue} using Redis and Postgres
 */
public class DistributedQueue implements JobQueue {

    private static final Logger logger = LoggerFactory.getLogger(DistributedQueue.class);
    private static final Double MILLIS_PER_SECOND = 1000.0;

    private final Queue<UUID> queue;
    private final Session session;

    @Inject
    DistributedQueue(RedissonClient client, Session session) {
        this.queue = client.getQueue("jobqueue");
        this.session = session;
    }

    @Override
    public void submitJob(UUID jobID, JobModel data) {
        assert(jobID == data.getJobID() && data.getStatus() == JobStatus.QUEUED);
        logger.debug("Adding jobID {} to the queue with for provider {}.", jobID, data.getProviderID());
        data.setSubmitTime(OffsetDateTime.now());
        // Persist the job in postgres
        final Transaction tx = this.session.beginTransaction();
        try {
            this.session.save(data);
            tx.commit();
        } catch (Exception e) {
            logger.error("Cannot add job to database", e);
            tx.rollback();
            throw new JobQueueFailure(jobID, e);
        }
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
    public Optional<JobModel> getJob(UUID jobID) {
        // Get from Postgres
        final Transaction tx = this.session.beginTransaction();
        try {
            final JobModel jobModel = this.session.get(JobModel.class, jobID);
            if (jobModel == null) {
                return Optional.empty();
            }
            this.session.refresh(jobModel);
            return Optional.ofNullable(jobModel);
        } finally {
            tx.commit();
        }
    }

    @Override
    public Optional<Pair<UUID, JobModel>> workJob() {
        final UUID jobID = this.queue.poll();
        if (jobID == null) {
            return Optional.empty();
        }
        final JobModel updatedJob = updateModel(jobID, (JobModel job) -> {
            // Verify that the job is in progress, otherwise fail
            if (job.getStatus() != JobStatus.QUEUED) {
                throw new JobQueueFailure(jobID, String.format("Cannot work job in state: %s", job.getStatus()));
            }

            // Update the status and start time
            job.setStatus(JobStatus.RUNNING);
            job.setStartTime(OffsetDateTime.now());
        });
        final var delay = Duration.between(updatedJob.getSubmitTime().get(), updatedJob.getStartTime().get()).toMillis()/MILLIS_PER_SECOND;
        logger.debug("Started work job {}, waited in queue for {} seconds", jobID, delay);

        return Optional.of(new Pair(jobID, updatedJob));
    }

    @Override
    public void completeJob(UUID jobID, JobStatus status, List<ResourceType> erringTypes) {
        assert(status == JobStatus.COMPLETED || status == JobStatus.FAILED);
        final JobModel updatedJob = updateModel(jobID, (JobModel job) -> {
            // Verify that the job is running
            if (job.getStatus() != JobStatus.RUNNING) {
                throw new JobQueueFailure(jobID, String.format("Cannot complete job in state: %s", job.getStatus()));
            }

            // Set the status and the complete time
            job.setStatus(status);
            job.setErringTypes(erringTypes);
            job.setCompleteTime(OffsetDateTime.now());
        });
        final var workDuration = Duration.between(updatedJob.getStartTime().get(), updatedJob.getCompleteTime().get()).toMillis()/MILLIS_PER_SECOND;
        logger.debug("Completed job {} with status {} and duration {} seconds", jobID, status, workDuration);
    }

    @Override
    public long queueSize() {
        return this.queue.size();
    }

    @Override
    public String queueType() {
        return "Redis Queue";
    }

    /**
     * Fetch the job from the database, call the mutator function to update the job, and save the update in the database.
     *
     * @param jobID - The jobID to fetch from the database.
     * @param mutator - Function called to update the job. If the mutator throws, rollback the transaction.
     * @return the {@link JobModel} after the a successful
     */
    private JobModel updateModel(UUID jobID, Consumer<JobModel> mutator) {
        final Transaction tx = this.session.beginTransaction();
        try {
            final JobModel jobModel = this.session.get(JobModel.class, jobID);
            if (jobModel == null) {
                throw new JobQueueFailure(jobID, "Unable to fetch job from database");
            }
            if (!jobModel.isValid()) {
                throw new JobQueueFailure(jobID, "Job fetched with an invalid values");
            }

            // Mutate the model
            mutator.accept(jobModel);

            this.session.update(jobModel);
            tx.commit();
            return jobModel;
        } catch (Exception e) {
            tx.rollback();
            logger.error("Unable to update job model", e);
            throw new JobQueueFailure(jobID, e);
        }
    }
}
