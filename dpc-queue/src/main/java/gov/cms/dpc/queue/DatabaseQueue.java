package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.annotations.HealthCheckQuery;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.exceptions.JobQueueUnhealthy;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
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
public class DatabaseQueue implements JobQueueInterface {

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
            @HealthCheckQuery String healthQuery,
            MetricRegistry metricRegistry
    ) {
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
    public void submitJob(JobQueueBatch job) {
        assert (job.getStatus() == JobStatus.QUEUED);
        logger.debug("Adding jobID {} batchID {} to the queue at {} with for organization {}.",
                job.getJobID(),
                job.getBatchID(),
                job.getSubmitTime().orElseThrow().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                job.getOrgID());

        // Persist the job in postgres
        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                session.persist(job);
                tx.commit();
            } catch (Exception e) {
                logger.error("Cannot add job to database", e);
                tx.rollback();
                throw new JobQueueFailure(job.getJobID(), job.getBatchID(), e);
            }
        }
    }

    @Override
    public Optional<JobQueueBatch> getJobBatch(UUID jobID, UUID batchID) {
        // Get from Postgres
        try (final Session session = this.factory.openSession()) {

            final Transaction tx = session.beginTransaction();
            try {
                final CriteriaBuilder builder = session.getCriteriaBuilder();
                final CriteriaQuery<JobQueueBatch> query = builder.createQuery(JobQueueBatch.class);
                final Root<JobQueueBatch> root = query.from(JobQueueBatch.class);

                query.select(root);
                query.where(
                        builder.equal(root.get("jobID"), jobID),
                        builder.equal(root.get("batchId"), batchID)
                );

                final JobQueueBatch job = session.createQuery(query).uniqueResult();
                if (job == null) {
                    return Optional.empty();
                }
                session.refresh(job);
                return Optional.of(job);
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
    public Optional<JobQueueBatch> workJob() {
        return Optional.empty();
    }

    @Override
    public void completeJob(JobQueueBatch job, List<JobQueueBatchFile> results) {

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
