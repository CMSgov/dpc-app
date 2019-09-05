package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.annotations.HealthCheckQuery;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements a distributed {@link gov.cms.dpc.queue.models.JobQueueBatch} using a Postgres database
 */
public class DatabaseQueue extends JobQueueCommon {

    // Statics
    private static final Logger logger = LoggerFactory.getLogger(DatabaseQueue.class);
    static final String DB_UNHEALTHY = "Database cluster is not responding";

    // Object variables
    private final SessionFactory factory;
    private final String healthQuery;

    // Metrics
    private final Timer waitTimer; // The wait time for a job to start
    private final Timer successTimer; // The work time a successful job takes
    private final Timer failureTimer; // The work time a failed job takes


    @Inject
    public DatabaseQueue(
            DPCManagedSessionFactory factory,
            @QueueBatchSize int batchSize,
            @HealthCheckQuery String healthQuery,
            MetricRegistry metricRegistry
    ) {
        super(batchSize);

        this.factory = factory.getSessionFactory();
        this.healthQuery = healthQuery;

        // Metrics
        final var metricBuilder = new MetricMaker(metricRegistry, DatabaseQueue.class);
        this.waitTimer = metricBuilder.registerTimer("waitTime");
        this.successTimer = metricBuilder.registerTimer("successTime");
        this.failureTimer = metricBuilder.registerTimer("failureTime");
        metricBuilder.registerCachedGauge("queueLength", this::queueSize);
    }

    @Override
    public void submitJobBatches(List<JobQueueBatch> jobBatches) {
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

                final List<JobQueueBatch> jobList = session.createQuery(query).getResultList();
                return jobList;
            } finally {
                tx.commit();
            }
        }
    }

    @Override
    public Optional<JobQueueBatch> workBatch(UUID aggregatorID) {
        return Optional.empty();
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
                // We just need to persist the job, as any results will be attached to the job and cascade
                session.persist(job);
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
    public void assertHealthy() {
        try (final Session session = this.factory.openSession()) {
            try {
                @SuppressWarnings("rawtypes") final Query healthCheck = session.createSQLQuery(healthQuery);
                healthCheck.getFirstResult();
            } catch (Exception e) {
                throw new JobQueueUnhealthy(DB_UNHEALTHY, e);
            }
        }
    }
}
