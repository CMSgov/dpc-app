package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.exceptions.DataRetrievalException;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.exceptions.JobQueueUnhealthy;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements a distributed {@link gov.cms.dpc.queue.models.JobQueueBatch} using a Postgres database
 */
public class DistributedBatchQueue extends JobQueueCommon {

    // Statics
    private static final Logger logger = LoggerFactory.getLogger(DistributedBatchQueue.class);
    private static final String DB_UNHEALTHY = "Database cluster is not responding";
    private static final String JOB_UNHEALTHY = "Aggregator is not making progress on the queue";

    // Object variables
    private final SessionFactory factory;

    // Metrics
    private final Timer waitTimer; // The wait time for a job to start
    private final Timer partialTimer; // The time to complete each partial of a batch
    private final Timer successTimer; // The work time a successful job takes
    private final Timer failureTimer; // The work time a failed job takes


    @Inject
    public DistributedBatchQueue(
            DPCQueueManagedSessionFactory factory,
            @QueueBatchSize int batchSize,
            MetricRegistry metricRegistry
    ) {
        super(batchSize);

        this.factory = factory.getSessionFactory();

        // Metrics
        final var metricBuilder = new MetricMaker(metricRegistry, DistributedBatchQueue.class);
        this.waitTimer = metricBuilder.registerTimer("waitTime");
        this.partialTimer = metricBuilder.registerTimer("partialTime");
        this.successTimer = metricBuilder.registerTimer("successTime");
        this.failureTimer = metricBuilder.registerTimer("failureTime");
        metricBuilder.registerCachedGauge("queueLength", this::queueSize);
    }

    @Override
    public void submitJobBatches(List<JobQueueBatch> jobBatches) {
        JobQueueBatch firstBatch = jobBatches.stream().findFirst().orElseThrow(() -> new JobQueueFailure("No job batches to submit"));

        logger.debug("Adding jobID {} ({} batches) to the queue at {} with for organization {}.",
                firstBatch.getJobID(),
                jobBatches.size(),
                firstBatch.getSubmitTime().orElseThrow(() -> new JobQueueFailure(firstBatch.getJobID(), firstBatch.getBatchID(), "The batches have not been prepared for submission")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                firstBatch.getOrgID());

        // Persist the batches in postgres
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                for ( JobQueueBatch batch : jobBatches ) {
                    session.persist(batch);
                }
                tx.commit();
            } catch (Exception e) {
                logger.error("Cannot add job batches to database", e);
                tx.rollback();
                throw new JobQueueFailure(firstBatch.getJobID(), firstBatch.getBatchID(), e);
            }
        }
    }

    @Override
    public Optional<JobQueueBatch> getBatch( UUID batchID) {
        // Get from Postgres
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                final JobQueueBatch batch = session.get(JobQueueBatch.class, batchID);
                if ( batch == null ) {
                    return Optional.empty();
                }
                session.refresh(batch);
                return Optional.of(batch);
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public List<JobQueueBatch> getJobBatches(UUID jobID) {
        // Get from Postgres
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                final CriteriaBuilder builder = session.getCriteriaBuilder();
                final CriteriaQuery<JobQueueBatch> query = builder.createQuery(JobQueueBatch.class);
                final Root<JobQueueBatch> root = query.from(JobQueueBatch.class);

                query.select(root);
                query.where(
                        builder.equal(root.get("jobID"), jobID)
                );

                return session.createQuery(query).getResultList();
            } finally {
                tx.commit();
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Optional<JobQueueBatchFile> getJobBatchFile(UUID organizationID, String fileID) {
        try (final Session session = this.factory.openSession()) {
            final String queryString =
                    "SELECT f FROM gov.cms.dpc.queue.models.JobQueueBatchFile f LEFT JOIN gov.cms.dpc.queue.models.JobQueueBatch b on b.jobID = f.jobID WHERE f.fileName = :fileName AND b.orgID = :org";

            final Query query = session.createQuery(queryString, JobQueueBatchFile.class);
            query.setParameter("fileName", fileID);
            query.setParameter("org", organizationID);
            return query.uniqueResultOptional();
        }
    }

    @Override
    public Optional<JobQueueBatch> claimBatch(UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                this.restartStuckBatches(session);
                return this.claimBatchFromDatabase(session, aggregatorID);
            } finally {
                tx.commit();
            }
        }
    }

    /**
     * Looks for any job batches that have stopped processing and are stuck in a running state. Restart those batches
     * so they can be picked up again.
     *
     * @param session - The active database session
     */
    private void restartStuckBatches(Session session) {
        // Find stuck batches
        List<String> stuckBatchIDs = session.createNativeQuery("SELECT Cast(batch_id as varchar) batch_id FROM job_queue_batch WHERE status = 1 AND update_time < current_timestamp - interval '15 minutes' FOR UPDATE SKIP LOCKED",
                        String.class)
                .getResultList();

        // Unstick stuck batches
        if ( stuckBatchIDs != null && !stuckBatchIDs.isEmpty() ) {
            final CriteriaBuilder builder = session.getCriteriaBuilder();
            final CriteriaQuery<JobQueueBatch> query = builder.createQuery(JobQueueBatch.class);
            final Root<JobQueueBatch> root = query.from(JobQueueBatch.class);

            query.select(root);
            query.where(root.get("batchID").in(stuckBatchIDs.stream().map(UUID::fromString).toList()));
            final List<JobQueueBatch> stuckJobList = session.createQuery(query).getResultList();

            for ( JobQueueBatch stuckJob : stuckJobList ) {
                logger.warn("Restarting stuck batch... batchID={}", stuckJob.getBatchID());
                stuckJob.restartBatch();
                session.merge(stuckJob);
            }
        }
    }


    /**
     * Claim a new batch to process from the database
     *
     * @param session - The active database session
     * @param aggregatorID - The ID of the aggregator processing the job
     * @return the claimed job batch
     */
    private Optional<JobQueueBatch> claimBatchFromDatabase(Session session, UUID aggregatorID) {
        // Claim a new batch
        Optional<String> batchID = session.createNativeQuery("SELECT Cast(batch_id as varchar) batch_id FROM job_queue_batch WHERE status = 0 ORDER BY priority ASC, submit_time ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
                        String.class)
                .uniqueResultOptional();

        if ( batchID.isPresent() ) {
            JobQueueBatch batch = session.get(JobQueueBatch.class, UUID.fromString(batchID.get()));
            try {
                batch.setRunningStatus(aggregatorID);
            } catch (Exception e) {
                logger.error("Failed to mark job as running. Marking the job as failed", e);
                batch.setFailedStatus();
                return Optional.empty();
            } finally {
                session.merge(batch);
            }

            final var delay = Duration.between(batch.getStartTime().orElseThrow(), batch.getUpdateTime().orElseThrow());
            waitTimer.update(delay.toMillis(), TimeUnit.MILLISECONDS);

            return Optional.of(batch);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void pauseBatch(JobQueueBatch job, UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                job.setPausedStatus(aggregatorID);
                session.merge(job);
            } finally {
                tx.commit();
            }
        } catch(Exception e) {
            logger.error("Error pausing batch: {} {}", job.getBatchID(), e.getMessage());
        }
    }

    @Override
    public void completePartialBatch(JobQueueBatch job, UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                final Optional<OffsetDateTime> lastUpdate = job.getUpdateTime();

                // We just need to persist the job, as any results will be attached to the job and cascade
                JobQueueBatch mergedJob = session.merge(job);

                final var delay = Duration.between(lastUpdate.orElseThrow(), job.getUpdateTime().orElseThrow());
                partialTimer.update(delay.toMillis(), TimeUnit.MILLISECONDS);

                mergedJob.setUpdateTime();
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public void completeBatch(JobQueueBatch job, UUID aggregatorID) {
        if ( job == null ) {
            throw new JobQueueFailure("Empty job passed");
        }

        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                job.setCompletedStatus(aggregatorID);
                session.merge(job);

                final var delay = Duration.between(job.getStartTime().orElseThrow(), job.getCompleteTime().orElseThrow());
                successTimer.update(delay.toMillis(), TimeUnit.MILLISECONDS);
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public void failBatch(JobQueueBatch job, UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                job.setFailedStatus();
                session.merge(job);

                final var delay = Duration.between(job.getStartTime().orElseThrow(), job.getUpdateTime().orElseThrow());
                failureTimer.update(delay.toMillis(), TimeUnit.MILLISECONDS);
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public long queueSize() {
        try (final Session session = this.factory.openSession()) {
            try {
                final CriteriaBuilder builder = session.getCriteriaBuilder();
                final CriteriaQuery<Long> query = builder.createQuery(Long.class);
                final Root<JobQueueBatch> root = query.from(JobQueueBatch.class);

                query.select(builder.count(root));
                query.where(
                        builder.equal(root.get("status"), JobStatus.QUEUED)
                );

                return session.createQuery(query).getSingleResult();
            } catch ( Exception e ) {
                return 0;
            }
        }
    }

    /**
     * Calculates the age in hours of the oldest job currently queued.
     * Note: submit_time is nullable in the job_queue_batch table.  If a row has a null submit_time it'll be excluded
     * from this query.
     * @return Age in hours of oldest job in the queue, 0 if there isn't a job waiting.
     */
    public double queueAge() {
        try (final Session session = this.factory.openSession()) {
            try {
                Optional<Timestamp> submitTime =
                    session.createNativeQuery("SELECT MIN( submit_time ) FROM job_queue_batch WHERE status = " + JobStatus.QUEUED.ordinal(),
                                    Timestamp.class)
                    .uniqueResultOptional();

                if(submitTime.isPresent()) {
                    Long now = Timestamp.from(Instant.now()).getTime(); // Now in milliseconds from Unix epoch
                    Long then = submitTime.get().getTime();             // Submit time in milliseconds from Unix epoch
                    return ((double) (now - then)) / (1000 * 60 * 60);  // msec difference / msec in an hour
                } else {
                    return 0;
                }
            } catch ( Exception e ) {
                throw new DataRetrievalException("Could not get queue age: ", e);
            }
        }
    }

    @Override
    public String queueType() {
        return "Database Queue";
    }

    @Override
    public void assertHealthy(UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            try {
                OffsetDateTime stuckSince = OffsetDateTime.now(ZoneId.systemDefault()).minusMinutes(3);

                logger.debug("Checking aggregatorID({}) for stuck jobs since ({})...", aggregatorID, stuckSince);
                Long stuckBatchCount = session
                        .createQuery("select count(*) from job_queue_batch where aggregatorID = :aggregatorID and status = 1 and updateTime < :updateTime", Long.class)
                        .setParameter("aggregatorID", aggregatorID)
                        .setParameter("updateTime", stuckSince)
                        .uniqueResult();

                logger.debug("Found ({}) stuck jobs on aggregatorID({}).", stuckBatchCount, aggregatorID);

                if (stuckBatchCount > 0) {
                    throw new JobQueueUnhealthy(JOB_UNHEALTHY);
                }
            } catch (JobQueueUnhealthy e) {
                // Rethrow
                throw e;
            } catch (Exception e) {
                throw new JobQueueUnhealthy(DB_UNHEALTHY, e);
            }
        }
    }
}
