package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

/**
 * Implements a distributed {@link JobQueue} using Redis and Postgres
 */
public class DistributedQueue implements JobQueue {

    private static final Logger logger = LoggerFactory.getLogger(DistributedQueue.class);

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

        // Fetch the Job from Postgres
        final Transaction tx = this.session.beginTransaction();

        try {
            final JobModel jobModel = this.session.get(JobModel.class, jobID);
            if (jobModel == null) {
                throw new JobQueueFailure(jobID, "Unable to fetch job from database");
            }

            // Verify that the job is in progress, otherwise fail
            if (jobModel.getStatus() != JobStatus.QUEUED) {
                throw new JobQueueFailure(jobID, String.format("Cannot work job in state: %s", jobModel.getStatus()));
            }

            // Update the status and persist it
            jobModel.setStatus(JobStatus.RUNNING);
            jobModel.setStartTime(OffsetDateTime.now());
            this.session.update(jobModel);
            tx.commit();

            return Optional.of(new Pair<>(jobID, jobModel));
        } catch (Exception e) {
            tx.rollback();
            logger.error("Cannot retrieve job from DB.", e);
            throw new JobQueueFailure(jobID, e);
        }
    }

    @Override
    public void completeJob(UUID jobID, JobStatus status) {
        assert(status == JobStatus.COMPLETED || status == JobStatus.FAILED);
        logger.debug("Completing job {} with status {}.", jobID, status);

        final Transaction tx = this.session.beginTransaction();
        try {
            final JobModel jobModel = this.session.get(JobModel.class, jobID);
            jobModel.setStatus(status);
            jobModel.setCompleteTime(OffsetDateTime.now());
            this.session.update(jobModel);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error("Unable to complete job", e);
            throw new JobQueueFailure(jobID, e);
        }
    }

    @Override
    public long queueSize() {
        return this.queue.size();
    }

    @Override
    public String queueType() {
        return "Redis Queue";
    }
}
