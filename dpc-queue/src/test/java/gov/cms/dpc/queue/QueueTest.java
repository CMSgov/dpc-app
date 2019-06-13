package gov.cms.dpc.queue;

import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.redisson.Redisson;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "rawtypes"})
public class QueueTest {

    //    private JobQueue queue;
    private SessionFactory sessionFactory;
    private List<String> queues = List.of("memory", "distributed");


    @TestFactory
    Stream<DynamicTest> testSource() {

        BiFunction<JobQueue, String, String> nameGenerator = (queue, operation) -> String.format("Testing operation: %s on queue: %s", operation, queue.queueType());
        return queues
                .stream()
                .map(queueName -> {
                    if (queueName.equals("memory")) {
                        return new MemoryQueue();
                    } else if (queueName.equals("distributed")) {
                        final Config config = new Config();
                        config.useSingleServer().setAddress("redis://localhost:6379");

                        final RedissonClient client = Redisson.create(config);
                        // Get the job queue and clear it
                        final RQueue<Object> jobQueue = client.getQueue("jobqueue");
                        jobQueue.clear();

                        // Create the session factory
                        final Configuration conf = new Configuration();
                        sessionFactory = conf.configure().buildSessionFactory();
                        return new DistributedQueue(client, new DPCManagedSessionFactory(sessionFactory), "SELECT 1");
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
    public void setupQueue() {

    }

    @AfterEach
    public void shutdown() {
        try (final Session session = sessionFactory.openSession()) {

            final Transaction tx = session.beginTransaction();
            try {
	            session.createQuery("delete from job_result").executeUpdate();
    	        session.createQuery("delete from job_queue").executeUpdate();
            } finally {
                tx.commit();
            }
        }
        sessionFactory.close();
    }

    public void testSimpleSubmissionCompletion(JobQueue queue) {
        // Add a couple of jobs
        final Set<UUID> jobSet = new HashSet<>();
        jobSet.add(UUID.randomUUID());
        jobSet.add(UUID.randomUUID());

        jobSet.forEach((job) -> queue.submitJob(job, QueueTest.buildModel(job)));
        assertEquals(2, queue.queueSize(), "Should have 2 jobs");

        // Check the status of the job
        final UUID firstJob = getSetFirst(jobSet);
        final Optional<JobModel> job = queue.getJob(firstJob);
        assertAll(() -> assertTrue(job.isPresent(), "Should be present in the queue."),
                () -> assertEquals(JobStatus.QUEUED, job.get().getStatus(), "Job should be in queue"));

        // Work the job
        Optional<Pair<UUID, JobModel>> workJob = queue.workJob();
        assertTrue(workJob.isPresent(), "Should have a job to work");

        // Check that the status is RUNNING
        final UUID firstID = workJob.get().getLeft();
        final Optional<JobModel> runningJob = queue.getJob(firstID);
        assertAll(() -> assertTrue(runningJob.isPresent(), "Should have a status"),
                () -> assertEquals(JobStatus.RUNNING, runningJob.get().getStatus(), "Job should be running"));
        runningJob.orElseThrow().addJobResult(new JobResult(firstID, ResourceType.Patient, 0, 1));

        // Complete the job
        queue.completeJob(workJob.get().getLeft(), JobStatus.COMPLETED, runningJob.get().getJobResults());

        // Check that the status is COMPLETED and with resource types
        final Optional<JobModel> completedJob = queue.getJob(workJob.get().getLeft());
        assertAll(() -> assertTrue(completedJob.isPresent(), "Should have a job"),
                () -> assertEquals(JobStatus.COMPLETED, completedJob.get().getStatus(), "Job should be completed"),
                () -> assertNotNull(job.get().getJobResults()));

        // Work the second job
        workJob = queue.workJob();
        assertTrue(workJob.isPresent(), "Should have a 2nd job to work");

        // Try to work again last job again, this should return no job
        var emptyJob = queue.workJob();
        assertTrue(emptyJob.isEmpty(), "The queue should not have ANY ready items");

        // Fail the second job and check its status
        final var secondJob = workJob.get().getRight();
        queue.completeJob(secondJob.getJobID(), JobStatus.FAILED, secondJob.getJobResults());
//        jobSet.remove(workJob.get().getLeft());

        Optional<JobModel> failedJob = queue.getJob(workJob.get().getLeft());
        assertAll(() -> assertTrue(failedJob.isPresent(), "Should have job in the queue"),
                () -> assertEquals(JobStatus.FAILED, failedJob.get().getStatus(), "Job should have failed"));

        // Remove some jobs
//        queue.removeJob(workJob.get().getLeft());
        assertEquals(0, queue.queueSize(), "Not have any jobs in the queue");
    }

    public void testPatientAndEOBSubmission(JobQueue queue) {
        // Add a job with a EOB resource
        final var jobID = UUID.randomUUID();
        final var jobSubmission = QueueTest.buildModel(jobID, ResourceType.Patient, ResourceType.ExplanationOfBenefit);
        queue.submitJob(jobID, jobSubmission);

        // Retrieve the job with both resources
        final var workJob = queue.workJob().orElseThrow().getRight();
        workJob.addJobResult(new JobResult(jobID, ResourceType.Patient, 0, 1));
        workJob.addJobResult(new JobResult(jobID, ResourceType.ExplanationOfBenefit, 0, 1));


        // Complete job
        queue.completeJob(workJob.getJobID(), JobStatus.COMPLETED, workJob.getJobResults());

        // Get the job and check its values
        final var actualJob = queue.getJob(jobID);
        assertTrue(actualJob.isPresent());
        actualJob.ifPresent(job -> {
            assertEquals(JobStatus.COMPLETED, job.getStatus());
            assertEquals(2, job.getJobResults().size());
            assertEquals(1, job.getJobResult(ResourceType.Patient).orElseThrow().getCount());
        });
    }

    public void testMissingJob(JobQueue queue) {
        UUID jobID = UUID.randomUUID();

        // Check that things are empty
        assertAll(() -> assertTrue(queue.workJob().isEmpty(), "Should not have a job to work"),
                () -> assertEquals(0, queue.queueSize(), "Should have an empty queue"));

        assertTrue(queue.getJob(jobID).isEmpty(), "Should not be able to get a missing job");
        assertThrows(JobQueueFailure.class, () -> queue.completeJob(jobID, JobStatus.FAILED, List.of()), "Should error when completing a job which does not exist");
    }

    private static <T> T getSetFirst(Set<T> set) {
        return set.stream().findFirst().orElseThrow(() -> new IllegalStateException("Cannot get first from empty array"));
    }

    private static JobModel buildModel(UUID id) {
        return buildModel(id, ResourceType.Patient);
    }

    private static JobModel buildModel(UUID id, ResourceType... resources) {
        return new JobModel(id, Arrays.asList(resources), "test-provider-1", List.of("test-patient-1", "test-patient-2"));
    }
}
