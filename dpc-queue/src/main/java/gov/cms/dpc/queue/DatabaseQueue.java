package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.exceptions.JobQueueUnhealthy;
import gov.cms.dpc.queue.models.JobQueueBatch;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements a distributed {@link gov.cms.dpc.queue.models.JobQueueBatch} using a Postgres database
 */
public class DatabaseQueue extends JobQueueCommon {

    // Statics
    private static final Logger logger = LoggerFactory.getLogger(DatabaseQueue.class);
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
    public DatabaseQueue(
            DPCManagedSessionFactory factory,
            @QueueBatchSize int batchSize,
            MetricRegistry metricRegistry
    ) {
        super(batchSize);

        this.factory = factory.getSessionFactory();

        // Metrics
        final var metricBuilder = new MetricMaker(metricRegistry, DatabaseQueue.class);
        this.waitTimer = metricBuilder.registerTimer("waitTime");
        this.partialTimer = metricBuilder.registerTimer("partialTime");
        this.successTimer = metricBuilder.registerTimer("successTime");
        this.failureTimer = metricBuilder.registerTimer("failureTime");
        metricBuilder.registerCachedGauge("queueLength", this::queueSize);
    }

    @Override
    protected void submitJobBatches(List<JobQueueBatch> jobBatches) {
        JobQueueBatch firstBatch = jobBatches.stream().findFirst().orElseThrow();

        logger.debug("Adding jobID {} ({} batches) to the queue at {} with for organization {}.",
                firstBatch.getJobID(),
                jobBatches.size(),
                firstBatch.getSubmitTime().orElseThrow().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
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

    @SuppressWarnings("unchecked")
    @Override
    public Optional<JobQueueBatch> workBatch(UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                // Find stuck batches
                List<String> stuckBatchIDs = session.createSQLQuery("SELECT batch_id FROM job_queue_batch WHERE status = 1 AND update_time > current_timestamp - interval '5 minutes' FOR UPDATE SKIP LOCKED")
                        .getResultList();

                // Unstick stuck batches
                if ( stuckBatchIDs != null && !stuckBatchIDs.isEmpty() ) {
                    final CriteriaBuilder builder = session.getCriteriaBuilder();
                    final CriteriaQuery<JobQueueBatch> query = builder.createQuery(JobQueueBatch.class);
                    final Root<JobQueueBatch> root = query.from(JobQueueBatch.class);

                    query.select(root);
                    query.where(root.get("batchID").in(stuckBatchIDs));
                    final List<JobQueueBatch> stuckJobList = session.createQuery(query).getResultList();

                    for ( JobQueueBatch stuckJob : stuckJobList ) {
                        stuckJob.restartBatch();
                        session.persist(stuckJob);
                    }
                }

                // Claim a new batch
                Optional<String> batchID = session.createSQLQuery("SELECT batch_id FROM job_queue_batch WHERE status = 0 ORDER BY priority ASC, submit_time ASC LIMIT 1 FOR UPDATE SKIP LOCKED")
                        .uniqueResultOptional()
                        .map(Object::toString);

                if ( batchID.isPresent() ) {
                    JobQueueBatch batch = session.get(JobQueueBatch.class, batchID.get());
                    batch.setRunningStatus(aggregatorID);
                    session.persist(batch);
                    session.refresh(batch);

                    final var delay = Duration.between(batch.getStartTime().orElseThrow(), batch.getUpdateTime().orElseThrow());
                    waitTimer.update(delay.toMillis(), TimeUnit.MILLISECONDS);

                    return Optional.of(batch);
                } else {
                    return Optional.empty();
                }
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public void pauseBatch(JobQueueBatch job, UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                job.setPausedStatus(aggregatorID);
                session.persist(job);
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public void completePartialBatch(JobQueueBatch job, UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                final Optional<OffsetDateTime> lastUpdate = job.getUpdateTime();

                // We just need to persist the job, as any results will be attached to the job and cascade
                session.persist(job);
                session.refresh(job);

                final var delay = Duration.between(lastUpdate.orElseThrow(), job.getUpdateTime().orElseThrow());
                partialTimer.update(delay.toMillis(), TimeUnit.MILLISECONDS);
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public void completeBatch(JobQueueBatch job, UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                job.setCompletedStatus(aggregatorID);
                session.persist(job);
                session.refresh(job);

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
                job.setFailedStatus(aggregatorID);
                session.persist(job);
                session.refresh(job);

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

    @Override
    public String queueType() {
        return "Database Queue";
    }

    @Override
    public void assertHealthy(UUID aggregatorID) {
        try (final Session session = this.factory.openSession()) {
            try {
                @SuppressWarnings("rawtypes") final Query healthCheck = session.createSQLQuery("select count(*) from job_queue_batch where aggregatorID = '" + aggregatorID.toString() + "' and job_status == 1 and update_time < current_timestamp - interval '3 minutes'");
                int stuckBatches = healthCheck.getFirstResult();
                if (stuckBatches > 0) {
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
