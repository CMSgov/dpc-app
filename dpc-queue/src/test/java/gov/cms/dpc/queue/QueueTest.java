package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "rawtypes"})
class QueueTest {

    //    private JobQueue queue;
    private SessionFactory sessionFactory;
    private List<String> queues = List.of("memory", "distributed");
    private final UUID aggregatorID = UUID.randomUUID();


    @TestFactory
    Stream<DynamicTest> testSource() {

        BiFunction<IJobQueue, String, String> nameGenerator = (queue, operation) -> String.format("Testing operation: %s on queue: %s", operation, queue.queueType());
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
                    return List.of(first, second, third);
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

    void testSimpleSubmissionCompletion(IJobQueue queue) {
        // One organization id for both jobs
        final UUID orgID = UUID.randomUUID();

        // Add a couple of jobs
        var firstJobID = queue.createJob(orgID, "test-provider-1", List.of("test-patient-1", "test-patient-2"), Arrays.asList(ResourceType.Patient));
        var secondJobID = queue.createJob(orgID, "test-provider-1", List.of("test-patient-1", "test-patient-2"), Arrays.asList(ResourceType.Patient));

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
        while ( workBatch.get().fetchNextPatient(aggregatorID).isPresent() ) {
            queue.completePartialBatch(workBatch.get(), aggregatorID);
        }
        workBatch.get().addJobQueueFile(ResourceType.Patient, 0, 1);
        queue.completeBatch(workBatch.get(), aggregatorID);

        // Check that the status is COMPLETED and with JobResults
        final Optional<JobQueueBatch> completedOptional = queue.getBatch(firstBatchID);
        completedOptional.ifPresent(completedJob -> {
            assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
            assertEquals(1, completedJob.getJobQueueBatchFiles().size());
            assertTrue(completedJob.getJobQueueFile(ResourceType.Patient).isPresent());
        });

        // Work the second job
        workBatch = queue.claimBatch(aggregatorID);
        assertTrue(workBatch.isPresent(), "Should have a 2nd job to work");

        // Try to work again last job again, this should return no job
        var emptyBatch = queue.claimBatch(aggregatorID);
        assertTrue(emptyBatch.isEmpty(), "The queue should not have ANY ready items");

        // Fail the second job and check its status
        final var secondBatch = workBatch.get();
        workBatch.get().addJobQueueFile(ResourceType.Patient, 0, 1);
        queue.failBatch(secondBatch, aggregatorID);

        // Check its persisted status
        Optional<JobQueueBatch> failedBatch = queue.getBatch(secondBatch.getBatchID());
        assertAll(() -> assertTrue(failedBatch.isPresent(), "Should have job in the queue"),
                () -> assertEquals(JobStatus.FAILED, failedBatch.orElseThrow().getStatus(), "Job should have failed"),
                () -> assertEquals(0, failedBatch.orElseThrow().getJobQueueBatchFiles().size(), "FAILED jobs should have empty results"));

        // After working two jobs the queue should be empty
        assertEquals(0, queue.queueSize(), "Worked all jobs in the queue, but the queue is not empty");
    }

    void testPatientAndEOBSubmission(IJobQueue queue) {
        // Add a job with a EOB resource
        final var orgID = UUID.randomUUID();
        final var jobID = queue.createJob(orgID, "test-provider-1", List.of("test-patient-1", "test-patient-2"), Arrays.asList(ResourceType.Patient, ResourceType.ExplanationOfBenefit));

        // Retrieve the job with both resources
        final var workBatch = queue.claimBatch(aggregatorID).get();
        workBatch.addJobQueueFile(ResourceType.Patient, 0, 1);
        workBatch.addJobQueueFile(ResourceType.ExplanationOfBenefit, 0, 1);

        // Complete job
        while ( workBatch.fetchNextPatient(aggregatorID).isPresent() ) {
            queue.completePartialBatch(workBatch, aggregatorID);
        }
        queue.completeBatch(workBatch, aggregatorID);

        // Get the job and check its values
        final var actualBatch = queue.getBatch(workBatch.getBatchID());
        assertTrue(actualBatch.isPresent());
        actualBatch.ifPresent(batch -> {
            assertEquals(JobStatus.COMPLETED, batch.getStatus());
            assertEquals(2, batch.getJobQueueBatchFiles().size());
            assertEquals(1, batch.getJobQueueFile(ResourceType.Patient).orElseThrow().getCount());
        });
    }

    void testMissingJob(IJobQueue queue) {
        UUID batchID = UUID.randomUUID();

        // Check that things are empty
        assertAll(() -> assertTrue(queue.claimBatch(aggregatorID).isEmpty(), "Should not have a job to work"),
                () -> assertEquals(0, queue.queueSize(), "Should have an empty queue"));

        assertTrue(queue.getBatch(batchID).isEmpty(), "Should not be able to get a missing batch");
        assertThrows(JobQueueFailure.class, () -> queue.completeBatch(null, aggregatorID), "Should error when completing a job which does not exist");
    }
}
