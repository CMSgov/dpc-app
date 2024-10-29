package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.query.MutationQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Distributed job queue")
class DistributedBatchQueueUnitTest extends AbstractMultipleDAOTest {
    DistributedBatchQueueUnitTest() {
            super(JobQueueBatch.class, JobQueueBatchFile.class);
    }

    private DPCQueueManagedSessionFactory sessionFactory;
    private DistributedBatchQueue queue;
    private Session session;

    @BeforeEach
    void setup() {
            sessionFactory = new DPCQueueManagedSessionFactory(db.getSessionFactory());
            queue = new DistributedBatchQueue(sessionFactory, 100, new MetricRegistry());
            session = sessionFactory.getSessionFactory().openSession();
    }

    @Test
    @DisplayName("Verify empty queue has no age 🥳")
    void test_queueAge_returns_0_on_empty() {
            Transaction transaction = session.beginTransaction();

            MutationQuery query = session.createMutationQuery("DELETE from job_queue_batch");
            query.executeUpdate();
            transaction.commit();

            assertEquals(0, queue.queueAge());
    }

    @Test
    @DisplayName("Verify populated queue has an age 🥳")
    void test_queueAge_works() {
            Transaction transaction = session.beginTransaction();

            long start = System.nanoTime();
            JobQueueBatch jobQueueBatch = new JobQueueBatch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "orgNpi",
                    "providerNpi",
                    List.of(),
                    List.of(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    "reqIp",
                    "reqUrl",
                    true
            );

            session.persist(jobQueueBatch);
            transaction.commit();
            long stop = System.nanoTime();

            double elapsedHours = (stop - start) / 1E9 * 3600;

            Double age = queue.queueAge();
            assertTrue(age > 0 && age < 2 * elapsedHours);
    }
}
