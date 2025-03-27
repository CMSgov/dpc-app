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
class DistributedBatchQueueTest {

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
                session.createMutationQuery("delete from job_queue_batch_file").executeUpdate();
                session.createMutationQuery("delete from job_queue_batch").executeUpdate();
            } finally {
                tx.commit();
            }
        }
        sessionFactory.close();
    }

    @Test
    void testHandleStuckBatchWithClaim() {
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
    void testValidateHealthyQueue() {
        // This test is kind of crappy, since there is nothing to assert
        // If the queue is not healthy, an exception is thrown
        assertDoesNotThrow(() -> queue.assertHealthy(aggregatorID));
    }

    @Test
    void testValidateUnhealthyQueueDueToJobFailure() {
        // One organization id for both jobs
        final UUID orgID = UUID.randomUUID();

        this.buildStuckBatchScenario(orgID);

        JobQueueUnhealthy e = assertThrows(JobQueueUnhealthy.class, () -> queue.assertHealthy(aggregatorID));
        assertEquals("Aggregator is not making progress on the queue", e.getMessage());
    }

    @Test
    void testPauseBatch() {
        UUID orgID = UUID.randomUUID();
        createJob(orgID);

        Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
        assertTrue(workBatch.isPresent());

        UUID firstBatchID = workBatch.get().getBatchID();
        Optional<JobQueueBatch> runningJob = queue.getBatch(firstBatchID);
        assertTrue(runningJob.isPresent());
        assertEquals(JobStatus.RUNNING, runningJob.get().getStatus());

        queue.pauseBatch(runningJob.get(), aggregatorID);
        Optional<JobQueueBatch> pausedJob = queue.getBatch(firstBatchID);
        assertTrue(pausedJob.isPresent());
        assertAll(
                () -> assertEquals(JobStatus.QUEUED, pausedJob.get().getStatus()),
                () -> assertTrue(pausedJob.get().getAggregatorID().isEmpty())
        );
    }

    private UUID createJob(UUID orgID) {
        return queue.createJob(orgID,
                NPIUtil.generateNPI(),
                NPIUtil.generateNPI(),
                List.of("test-patient-1", "test-patient-2"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null, true, false);
    }

    private UUID buildStuckBatchScenario(UUID orgID) {
        // Add a job
        var jobID = createJob(orgID);

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
                session.createMutationQuery("update job_queue_batch set updateTime = :updateTime where jobID = :jobID")
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
