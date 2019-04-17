package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.*;
import org.redisson.Redisson;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class QueueTest {

//    private JobQueue queue;
    private SessionFactory sessionFactory;
    private Session session;
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
                        session = sessionFactory.openSession();
                        return new DistributedQueue(client, session);
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
        final Transaction tx = session.beginTransaction();
        try {
            final Query query = session.createQuery("delete from job_queue");
            query.executeUpdate();
        } finally {
            tx.commit();
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
        final Optional<JobStatus> jobStatus = queue.getJobStatus(firstJob);
        assertAll(() -> assertTrue(jobStatus.isPresent(), "Should have a matching status"),
                () -> assertEquals(JobStatus.QUEUED, jobStatus.get(), "Job should be queue"));

        // Complete the job and check its status

        Optional<Pair<UUID, JobModel>> workJob = queue.workJob();
        assertTrue(workJob.isPresent(), "Should have job to work");
        // Check that the status is RUNNING
        final Optional<JobStatus> status = queue.getJobStatus(workJob.get().getLeft());
        assertAll(() -> assertTrue(status.isPresent(), "Should have Job status"),
                () -> assertEquals(JobStatus.RUNNING, status.get(), "Job should be running"));
        queue.completeJob(workJob.get().getLeft(), JobStatus.COMPLETED);

        final Optional<JobStatus> updatedStatus = queue.getJobStatus(workJob.get().getLeft());
        assertAll(() -> assertTrue(updatedStatus.isPresent(), "Should have job status"),
                () -> assertEquals(JobStatus.COMPLETED, updatedStatus.get(), "Job should be completed"));

        // Fail the job and check its status
        workJob = queue.workJob();
        assertTrue(workJob.isPresent(), "Should have a 2nd job to work");
        queue.completeJob(workJob.get().getLeft(), JobStatus.FAILED);
//        jobSet.remove(workJob.get().getLeft());

        Optional<JobStatus> failedStatus = queue.getJobStatus(workJob.get().getLeft());
        assertAll(() -> assertTrue(failedStatus.isPresent(), "Should have job status"),
                () -> assertEquals(JobStatus.FAILED, failedStatus.get(), "Job should have failed"));

        // Remove some jobs
//        queue.removeJob(workJob.get().getLeft());
        assertEquals(0, queue.queueSize(), "Not have any jobs in the queue");
    }

    public void testPatientAndEOBSubmission(JobQueue queue) {
        // Add a job with a EOB resource
        final var jobSubmission = QueueTest.buildModel(UUID.randomUUID(), ResourceType.Patient, ResourceType.ExplanationOfBenefit);
        queue.submitJob(jobSubmission.getJobID(), jobSubmission);

        // Retrieve the job with both resources
        final var jobRetrived = queue.workJob();
        assertTrue(jobRetrived.isPresent());
        final var resourcesRetrived = jobRetrived.get().getRight().getResourceTypes();
        assertTrue(resourcesRetrived.containsAll(List.of(ResourceType.Patient, ResourceType.ExplanationOfBenefit)));

        // Complete job
        queue.completeJob(jobSubmission.getJobID(), JobStatus.COMPLETED);
    }

    public void testMissingJob(JobQueue queue) {
        UUID jobID = UUID.randomUUID();

        // Check that things are empty
        assertAll(() -> assertTrue(queue.workJob().isEmpty(), "Should not have job to work"),
                () -> assertEquals(0, queue.queueSize(), "Should have an empty queue"));

        assertTrue(queue.getJobStatus(jobID).isEmpty(), "Should not be able to get missing job status");
        assertThrows(JobQueueFailure.class, () -> queue.completeJob(jobID, JobStatus.FAILED), "Should error when completing a job which does not exist");
    }

    private static <T> T getSetFirst(Set<T> set) {
        return set.stream().findFirst().orElseThrow(() -> new IllegalStateException("Cannot get first from empty array"));
    }

    private static class TestJob implements Statisable {

        private final String data;
        private JobStatus status;

        TestJob(String data) {
            this.data = data;
            this.status = JobStatus.QUEUED;
        }

        @Override
        public JobStatus getStatus() {
            return this.status;
        }

        @Override
        public void setStatus(JobStatus status) {
            this.status = status;
        }
    }

    private static JobModel buildModel(UUID id) {
        return buildModel(id, ResourceType.Patient );
    }

    private static JobModel buildModel(UUID id, ResourceType ... resources) {
        return new JobModel(id, Arrays.asList(resources), "test-provider-1", List.of("test-patient-1", "test-patient-2"));
    }
}
