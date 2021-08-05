package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"OptionalGetWithoutIsPresent"})
@ExtendWith(BufferedLoggerHandler.class)
@IntegrationTest
class QueueTest {

    //    private JobQueue queue;
    private SessionFactory sessionFactory;
    private final List<String> queues = List.of("memory", "distributed");
    private final UUID aggregatorID = UUID.randomUUID();
    private final UUID orgID = UUID.randomUUID();
    private final String orgNPI = NPIUtil.generateNPI();
    private final String providerNPI = NPIUtil.generateNPI();
    private final List<String> patientMBIs = List.of("test-patient-1", "test-patient-2");

    @TestFactory
    Stream<DynamicTest> testSource() {

        BiFunction<JobQueueCommon, String, String> nameGenerator = (queue, operation) -> String.format("Testing operation: %s on queue: %s", operation, queue.queueType());
        return queues
                .stream()
                .map(queueName -> {
                    if (queueName.equals("memory")) {
                        return new MemoryBatchQueue(100);
                    } else if (queueName.equals("distributed")) {
                        // Create the session factory
                        final Configuration conf = new Configuration();
                        sessionFactory = conf.configure().buildSessionFactory();
                        return new DistributedBatchQueue(new DPCQueueManagedSessionFactory(sessionFactory), 100, new MetricRegistry());
                    } else {
                        throw new IllegalArgumentException("I'm not that kind of queue");
                    }
                })
                .map(queue -> {
                    final DynamicTest first = DynamicTest.dynamicTest(nameGenerator.apply(queue, "Simple Submission"), () -> testSimpleSubmissionCompletion(queue));
                    final DynamicTest second = DynamicTest.dynamicTest(nameGenerator.apply(queue, "Missing Job"), () -> testMissingJob(queue));
                    final DynamicTest third = DynamicTest.dynamicTest(nameGenerator.apply(queue, "EOB Submission"), () -> testPatientAndEOBSubmission(queue));
                    final DynamicTest fourth = DynamicTest.dynamicTest(nameGenerator.apply(queue, "Invalid batch on queue"), () -> testInvalidJobBatch(queue));
                    final DynamicTest fifth = DynamicTest.dynamicTest(nameGenerator.apply(queue, "since equal transaction time"), () -> testSinceEqualTransactionTime(queue));
                    return List.of(first, second, third, fourth, fifth);
                })
                .flatMap(Collection::stream);
    }

    @BeforeEach
    void setupQueue() {

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

    void testSimpleSubmissionCompletion(JobQueueCommon queue) {
        // Add a couple of jobs
        var firstJobID = queue.createJob(orgID, orgNPI, providerNPI, patientMBIs, Collections.singletonList(DPCResourceType.Patient), null, OffsetDateTime.now(ZoneOffset.UTC), null, null,true);
        var secondJobID = queue.createJob(orgID, orgNPI, providerNPI, patientMBIs, Collections.singletonList(DPCResourceType.Patient), null, OffsetDateTime.now(ZoneOffset.UTC), null, null,true);
        assertEquals(firstJobID.getClass(), UUID.class);
        assertEquals(secondJobID.getClass(), UUID.class);
        assertEquals(2, queue.queueSize(), "Should have 2 jobs");

        // Check the status of the job
        final Optional<JobQueueBatch> job = queue.getJobBatches(firstJobID).stream().findFirst();
        assertAll(() -> assertTrue(job.isPresent(), "Should be present in the queue."),
                () -> assertEquals(JobStatus.QUEUED, job.get().getStatus(), "Job should be in queue"));

        // Work the job
        Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
        assertTrue(workBatch.isPresent(), "Should have a job to work");
        final UUID firstBatchID = workBatch.orElseThrow().getBatchID();

        // Check that the persisted job is RUNNING
        final Optional<JobQueueBatch> runningJob = queue.getBatch(firstBatchID);
        assertAll(() -> assertTrue(runningJob.isPresent(), "Should have a status"),
                () -> assertEquals(JobStatus.RUNNING, runningJob.orElseThrow().getStatus(), "Job should be running"));

        // Complete the job
        while (workBatch.get().fetchNextPatient(aggregatorID).isPresent()) {
            queue.completePartialBatch(workBatch.get(), aggregatorID);
        }
        workBatch.get().addJobQueueFile(DPCResourceType.Patient, 0, 1);
        queue.completeBatch(workBatch.get(), aggregatorID);

        // Check that the status is COMPLETED and with JobResults
        final Optional<JobQueueBatch> completedOptional = queue.getBatch(firstBatchID);
        assertTrue(completedOptional.isPresent(), "Should have job result");
        completedOptional.ifPresent(completedJob -> {
            assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
            assertEquals(1, completedJob.getJobQueueBatchFiles().size());
            assertTrue(completedJob.getJobQueueFile(DPCResourceType.Patient).isPresent());
        });

        // Verify we can match the batches correctly
        final String fileName = completedOptional.get().getJobQueueFile(DPCResourceType.Patient).get().getFileName();
        final Optional<JobQueueBatchFile> jobBatchFile = queue.getJobBatchFile(orgID, fileName);
        assertAll(() -> assertTrue(jobBatchFile.isPresent(), "Should have batch file"),
                () -> assertEquals(DPCResourceType.Patient, jobBatchFile.get().getResourceType(), "Should be a patient resource"));

        // Try with bad file ID and Org ID
        assertFalse(queue.getJobBatchFile(orgID, "not a real file").isPresent(), "Should not find file");
        assertFalse(queue.getJobBatchFile(UUID.randomUUID(), fileName).isPresent(), "Should not find file");

        // Work the second job
        workBatch = queue.claimBatch(aggregatorID);
        assertTrue(workBatch.isPresent(), "Should have a 2nd job to work");

        // Try to work again last job again, this should return no job
        var emptyBatch = queue.claimBatch(aggregatorID);
        assertTrue(emptyBatch.isEmpty(), "The queue should not have ANY ready items");

        // Fail the second job and check its status
        final var secondBatch = workBatch.get();
        workBatch.get().addJobQueueFile(DPCResourceType.Patient, 0, 1);
        queue.failBatch(secondBatch, aggregatorID);

        // Check its persisted status
        Optional<JobQueueBatch> failedBatch = queue.getBatch(secondBatch.getBatchID());
        assertAll(() -> assertTrue(failedBatch.isPresent(), "Should have job in the queue"),
                () -> assertEquals(JobStatus.FAILED, failedBatch.orElseThrow().getStatus(), "Job should have failed"),
                () -> assertEquals(0, failedBatch.orElseThrow().getJobQueueBatchFiles().size(), "FAILED jobs should have empty results"));

        // After working two jobs the queue should be empty
        assertEquals(0, queue.queueSize(), "Worked all jobs in the queue, but the queue is not empty");
    }

    void testPatientAndEOBSubmission(JobQueueCommon queue) {
        // Add a job with a EOB resource
        final var jobID = queue.createJob(orgID, orgNPI, providerNPI, patientMBIs,
                Arrays.asList(DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit),
                null,
                OffsetDateTime.now(ZoneOffset.UTC), null, null,true);
        assertEquals(jobID.getClass(), UUID.class);
        // Retrieve the job with both resources
        final var workBatch = queue.claimBatch(aggregatorID).get();
        workBatch.addJobQueueFile(DPCResourceType.Patient, 0, 1);
        workBatch.addJobQueueFile(DPCResourceType.ExplanationOfBenefit, 0, 1);

        // Complete job
        while (workBatch.fetchNextPatient(aggregatorID).isPresent()) {
            queue.completePartialBatch(workBatch, aggregatorID);
        }
        queue.completeBatch(workBatch, aggregatorID);

        // Get the job and check its values
        final var actualBatch = queue.getBatch(workBatch.getBatchID());
        assertTrue(actualBatch.isPresent());
        actualBatch.ifPresent(batch -> {
            assertEquals(JobStatus.COMPLETED, batch.getStatus());
            assertEquals(2, batch.getJobQueueBatchFiles().size());
            assertEquals(1, batch.getJobQueueFile(DPCResourceType.Patient).orElseThrow().getCount());
        });
    }

    void testMissingJob(JobQueueCommon queue) {
        UUID batchID = UUID.randomUUID();

        // Check that things are empty
        assertAll(() -> assertTrue(queue.claimBatch(aggregatorID).isEmpty(), "Should not have a job to work"),
                () -> assertEquals(0, queue.queueSize(), "Should have an empty queue"));

        assertTrue(queue.getBatch(batchID).isEmpty(), "Should not be able to get a missing batch");
        assertThrows(JobQueueFailure.class, () -> queue.completeBatch(null, aggregatorID), "Should error when completing a job which does not exist");
    }


    void testSinceEqualTransactionTime(JobQueueCommon queue) {
        final var transactionTime = OffsetDateTime.now(ZoneOffset.UTC);
        final var jobId = queue.createJob(orgID, orgNPI, providerNPI, patientMBIs,
                Arrays.asList(DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit),
                transactionTime,
                transactionTime, null, null,true);

        // Check that the Job has a empty queue
        final Optional<JobQueueBatch> job = queue.getJobBatches(jobId).stream().findFirst();
        assertAll(() -> assertTrue(job.isPresent(), "Should be present in the queue."),
                () -> assertEquals(JobStatus.QUEUED, job.get().getStatus(), "Job should be in queue"),
                () -> assertTrue(job.get().getPatients().isEmpty()));
    }

    void testInvalidJobBatch(JobQueueCommon queue) {
        final UUID jobID = UUID.randomUUID();

        final var jobBatch = new JobQueueBatch(
                jobID,
                orgID,
                orgNPI,
                providerNPI,
                patientMBIs,
                Collections.singletonList(DPCResourceType.ExplanationOfBenefit),
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                null,
                true);

        // Set the aggregatorID to something random so it gets claimed incorrectly
        jobBatch.setAggregatorIDForTesting(UUID.randomUUID());

        // Submit the job batch
        queue.submitJobBatches(Collections.singletonList(jobBatch));

        // Claim the batch, but expect it to fail and not return
        Optional<JobQueueBatch> claimed = queue.claimBatch(aggregatorID);
        assertFalse(claimed.isPresent(), "Invalid batch should not be claimed");

        // Verify the batch was marked as failed in the queue
        final var failedBatch = queue.getBatch(jobBatch.getBatchID());
        assertEquals(JobStatus.FAILED, failedBatch.get().getStatus());
    }
}
