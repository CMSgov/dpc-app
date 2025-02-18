package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

class AwsDistributedBatchQueueUnitTest {
    private final Session session = mock(Session.class);
	private final SessionFactory sessionFactory = mock(SessionFactory.class);
	private final Slf4jReporter ageReporter = mock(Slf4jReporter.class);
	private final Slf4jReporter sizeReporter = mock(Slf4jReporter.class);

	@Test
	void testReporterStarted() {
        MetricRegistry metricRegistry = new MetricRegistry();
        DPCAwsQueueConfiguration config = new DPCAwsQueueConfiguration()
                .setQueueSizeMetricName("sizeMetricName")
                .setQueueAgeMetricName("ageMetricName")
                .setEnvironment("env")
                .setAwsAgeReportingInterval(60)
                .setAwsSizeReportingInterval(60);

		when(sessionFactory.openSession()).thenReturn(session);

        new AwsDistributedBatchQueue(
                new DPCQueueManagedSessionFactory(sessionFactory),
                100,
                metricRegistry,
                ageReporter,
                sizeReporter,
                config
        );

		verify(ageReporter, times(1)).start(60, TimeUnit.SECONDS);
		verify(sizeReporter, times(1)).start(60, TimeUnit.SECONDS);
	}

}
