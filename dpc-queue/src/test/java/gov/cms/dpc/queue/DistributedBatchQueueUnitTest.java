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

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void test_queueAge_returns_0_on_empty() {
		Transaction transaction = session.beginTransaction();
		session.createMutationQuery("DELETE from job_queue_batch").executeUpdate();
		transaction.commit();

		assertEquals(0, queue.queueAge());
	}

	@Test
	void test_queueAge_works() {
		Transaction transaction = session.beginTransaction();

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

		Double age = queue.queueAge();

		// Check that our submitted job is between 0 and 0.001 hours old (about 3.6 seconds).
		// We could probably mock the system time and the results coming back from Hibernate, but this is enough to
		// prove it works.
		assertTrue(age > 0 && age < .001);
	}

	@Test
	void test_completePartialBatch_sets_update_time() throws InterruptedException {
		Transaction transaction = session.beginTransaction();

		UUID aggregatorId = UUID.randomUUID();
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

		jobQueueBatch.setAggregatorIDForTesting(aggregatorId);
		jobQueueBatch.setRunningStatus(aggregatorId);
		jobQueueBatch.setUpdateTime();
		OffsetDateTime initialUpdateTime = jobQueueBatch.getUpdateTime().get();
		session.persist(jobQueueBatch);
		transaction.commit();

		sleep(500);
		queue.completePartialBatch(jobQueueBatch, UUID.randomUUID());
		session.refresh(jobQueueBatch);
		OffsetDateTime retrievedUpdateTime = jobQueueBatch.getUpdateTime().get();

		assertTrue(retrievedUpdateTime.compareTo(initialUpdateTime) > 0);
	}
}
