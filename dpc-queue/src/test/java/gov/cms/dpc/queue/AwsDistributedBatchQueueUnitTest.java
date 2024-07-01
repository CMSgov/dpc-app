package gov.cms.dpc.queue;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

class AwsDistributedBatchQueueUnitTest {
	private AwsDistributedBatchQueue queue;
	private MetricRegistry metricRegistry;
	private DPCAwsQueueConfiguration config;
	private final Session session = mock(Session.class);
	private final SessionFactory sessionFactory = mock(SessionFactory.class);
	private final ConsoleReporter consoleReporter = mock(ConsoleReporter.class);

	@Test
	void testReporterStarted() {
		metricRegistry = new MetricRegistry();
		config = new DPCAwsQueueConfiguration()
			.setQueueSizeMetricName("name")
			.setEnvironment("env")
			.setAwsReporitingInterval(60);

		when(sessionFactory.openSession()).thenReturn(session);

		queue = new AwsDistributedBatchQueue(
			new DPCQueueManagedSessionFactory(sessionFactory),
			100,
			metricRegistry,
			consoleReporter,
			config
			);

		verify(consoleReporter, times(1)).start(60, TimeUnit.SECONDS);
	}

}
