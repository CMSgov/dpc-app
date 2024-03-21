package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.exceptions.JobQueueUnhealthy;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
@IntegrationTest
public class DistributedBatchQueueTest {

    private final UUID aggregatorID = UUID.randomUUID();
    private SessionFactory sessionFactory;
    private DistributedBatchQueue queue;

    @BeforeEach
    void setUp() {
        final Configuration conf = new Configuration();
        sessionFactory = conf.configure().buildSessionFactory();
        queue = new DistributedBatchQueue(new DPCQueueManagedSessionFactory(sessionFactory), 100, new MetricRegistry());
    }

    @AfterEach
    void shutdown() {
        try (final Session session = sessionFactory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                session.createQuery("delete from job_queue_batch_file").executeUpdate();
                session.createQuery("delete from job_queue_batch").executeUpdate();
            } finally {
                tx.commit();
            }
        }
        sessionFactory.close();
    }

    @Test
    void handleStuckBatchWithClaim() {
        // One organization id for both jobs
        final UUID orgID = UUID.randomUUID();

        final UUID firstBatchID = this.buildStuckBatchScenario(orgID);

        // Fix the stuck job during the claim process
        Optional<JobQueueBatch> stuckBatch = queue.claimBatch(aggregatorID);
        assertFalse(stuckBatch.isPresent(), "Should have no job, but the stuck batch was released and is ready to be re-claimed");

        // Re-claim the batch that was in a stuck state
        stuckBatch = queue.claimBatch(aggregatorID);
        assertTrue(stuckBatch.isPresent(), "Should have a job to work");
        final UUID stuckBatchID = stuckBatch.orElseThrow().getBatchID();
        assertEquals(stuckBatchID, firstBatchID, "Stuck batch should be the same as the initial batch");

        // Check that the stuck job is RUNNING with previous files cleared
        final Optional<JobQueueBatch> stuckJobOptional = queue.getBatch(stuckBatchID);
        assertTrue(stuckJobOptional.isPresent(), "Should have a job");
        stuckJobOptional.ifPresent(stuckJob -> {
            assertEquals(JobStatus.RUNNING, stuckJob.getStatus(), "Should be in the RUNNING state");
            assertEquals(0, stuckJob.getJobQueueBatchFiles().size(), "Should have no files on the stuck job (they should be cleared)");
        });
    }

    @Test
    void validateHealthyQueue() {
        // This test is kind of crappy, since there is nothing to assert
        // If the queue is not health, an exception is thrown
        queue.assertHealthy(aggregatorID);
    }

    @Test
    void validateUnhealthyQueueDueToJobFailure() {
        // One organization id for both jobs
        final UUID orgID = UUID.randomUUID();

        this.buildStuckBatchScenario(orgID);

        try {
            queue.assertHealthy(aggregatorID);
            fail("Expected JobQueueUnhealthy exception not throw");
        } catch (JobQueueUnhealthy e) {
            assertEquals("Aggregator is not making progress on the queue", e.getMessage());
        }
    }

    private UUID buildStuckBatchScenario(UUID orgID) {
        // Add a job
        var jobID = queue.createJob(orgID,
                NPIUtil.generateNPI(),
                NPIUtil.generateNPI(),
                List.of("test-patient-1", "test-patient-2"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null,true, false);

        // Work the job
        Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
        assertTrue(workBatch.isPresent(), "Should have a job to work");
        final UUID firstBatchID = workBatch.orElseThrow().getBatchID();

        // Add a file on the batch
        workBatch.get().addJobQueueFile(DPCResourceType.Patient, 0, 1);
        queue.completePartialBatch(workBatch.get(), aggregatorID);

        // Check that the persisted job is RUNNING
        final Optional<JobQueueBatch> runningJobOptional = queue.getBatch(firstBatchID);
        assertTrue(runningJobOptional.isPresent(), "Should have a running job");
        runningJobOptional.ifPresent(runningJob -> {
            assertEquals(JobStatus.RUNNING, runningJob.getStatus(), "Should be in the RUNNING state");
            assertEquals(1, runningJob.getJobQueueBatchFiles().size(), "Should have 1 file on the running job");
            assertTrue(runningJob.getJobQueueFile(DPCResourceType.Patient).isPresent(), "Should have a patient job file");
        });

        // Simulate a stuck job by modifying the update_time
        try (final Session session = sessionFactory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                session.createQuery("update job_queue_batch set updateTime = :updateTime where jobID = :jobID")
                        .setParameter("jobID", jobID)
                        .setParameter("updateTime", OffsetDateTime.now().minusMinutes(15))
                        .executeUpdate();
            } finally {
                tx.commit();
            }
        }

        return firstBatchID;
    }
}
