package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.queue.models.JobQueueBatchFilterClaim;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DistributedBatchQueueUnitTest extends AbstractMultipleDAOTest {
	DistributedBatchQueueUnitTest() {
		super(JobQueueBatch.class, JobQueueBatchFile.class, JobQueueBatchFilterClaim.class);
	}

	private DistributedBatchQueue queue;
	private Session session;
    private JobQueueBatch jobQueueBatch;

	@BeforeEach
	void setup() {
		DPCQueueManagedSessionFactory sessionFactory = new DPCQueueManagedSessionFactory(db.getSessionFactory());
		queue = new DistributedBatchQueue(sessionFactory, 100, new MetricRegistry());
		session = sessionFactory.getSessionFactory().openSession();
        jobQueueBatch = new JobQueueBatch.Builder()
                .jobID(UUID.randomUUID())
                .orgID(UUID.randomUUID())
                .orgNPI("orgNpi")
                .providerNPI("providerNpi")
                .patients(List.of())
                .resourceTypes(List.of())
                .since(OffsetDateTime.now())
                .transactionTime(OffsetDateTime.now(ZoneOffset.UTC))
                .requestingIP("reqIp")
                .requestUrl("reqUrl")
                .isBulk(true)
                .build();
	}

	@Test
	void test_queueAge_returns_0_on_empty() {
		Transaction transaction = session.beginTransaction();
		session.createMutationQuery("DELETE from JobQueueBatch").executeUpdate();
		transaction.commit();

		assertEquals(0, queue.queueAge());
	}

	@Test
	void test_queueAge_works() {
		Transaction transaction = session.beginTransaction();

		session.persist(jobQueueBatch);
		transaction.commit();

		double age = queue.queueAge();

		// Check that our submitted job is between 0 and 0.001 hours old (about 3.6 seconds).
		// We could probably mock the system time and the results coming back from Hibernate, but this is enough to
		// prove it works.
		assertTrue(age > 0 && age < .001);
	}

	@Test
	void test_queueType_works() {
		assertEquals("Database Queue", queue.queueType());
	}

	@Test
	void test_queueSize_works() {
		 assertEquals(0, queue.queueSize());

		Transaction transaction = session.beginTransaction();
		session.persist(jobQueueBatch);
		transaction.commit();

		assertEquals(1, queue.queueSize());
    }

	@Test
	void test_completePartialBatch_sets_update_time() {
		Transaction transaction = session.beginTransaction();

		UUID aggregatorId = UUID.randomUUID();
		jobQueueBatch.setAggregatorIDForTesting(aggregatorId);
		jobQueueBatch.setRunningStatus(aggregatorId);
		jobQueueBatch.setUpdateTime();
		OffsetDateTime initialUpdateTime = jobQueueBatch.getUpdateTime().get();
		session.persist(jobQueueBatch);
		transaction.commit();

		queue.completePartialBatch(jobQueueBatch, UUID.randomUUID());
		session.refresh(jobQueueBatch);
		OffsetDateTime retrievedUpdateTime = jobQueueBatch.getUpdateTime().get();

		assertTrue(retrievedUpdateTime.isAfter(initialUpdateTime));
	}

	@Test
	void test_getJobBatchFile_handles_parsing_exception() {
		UUID orgId = UUID.randomUUID();
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> queue.getJobBatchFile(orgId, "bad_file"));
		assertEquals("Could not parse file name: bad_file", e.getMessage());
	}

	@Test
	void test_getJobBatchFile_works() {
		Transaction transaction = session.beginTransaction();

		UUID batchId = jobQueueBatch.getBatchID();
        UUID orgId = jobQueueBatch.getOrgID();
        UUID jobId = jobQueueBatch.getJobID();

		int sequence = 0;
		DPCResourceType resource = DPCResourceType.ExplanationOfBenefit;
		JobQueueBatchFile jobQueueBatchFile = new JobQueueBatchFile(
			jobId,
			batchId,
			resource,
			sequence,
			0
		);

		session.persist(jobQueueBatch);
		session.persist(jobQueueBatchFile);
		transaction.commit();

		String fileName = JobQueueBatchFile.formOutputFileName(batchId, resource, sequence);
		Optional<JobQueueBatchFile> optionalJobQueueBatchFile = queue.getJobBatchFile(orgId, fileName);
		assertTrue(optionalJobQueueBatchFile.isPresent());

		JobQueueBatchFile retrievedJobQueueBatchFile = optionalJobQueueBatchFile.get();
		assertEquals(fileName, retrievedJobQueueBatchFile.getFileName());
		assertEquals(batchId, retrievedJobQueueBatchFile.getBatchID());
		assertEquals(jobId, retrievedJobQueueBatchFile.getJobID());
	}
}
