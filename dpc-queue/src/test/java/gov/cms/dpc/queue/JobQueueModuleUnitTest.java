package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.annotations.JobTimeout;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import gov.cms.dpc.queue.config.DPCQueueConfig;
import io.dropwizard.core.Configuration;
import io.github.azagniotov.metrics.reporter.cloudwatch.CloudWatchReporter;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import javax.inject.Singleton;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobQueueModuleUnitTest {
	JobQueueModule queueModule = spy(JobQueueModule.class);
	DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);
	MockConfig mockConfig = mock(MockConfig.class);

	MetricRegistry metricRegistry = new MetricRegistry();

	@BeforeEach
	void setup() throws NoSuchMethodException {
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);
		when(awsConfig.getAwsRegion()).thenReturn(Region.US_EAST_1.toString());
		when(awsConfig.getEnvironment()).thenReturn("env");
		when(awsConfig.getQueueSizeMetricName()).thenReturn("metricName");
		when(awsConfig.getAwsNamespace()).thenReturn("namespace");
		when(awsConfig.getAwsReportingInterval()).thenReturn(15);
	}

	@Test
	void test_provideBatchSize() {
		JobQueueModule queueModule = new JobQueueModule();
		assertEquals(100, queueModule.provideBatchSize());
	}

	@Test
	void test_provideAggregatorId() {
		UUID testUUID = UUID.randomUUID();

		try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
			uuid.when(UUID::randomUUID).thenReturn(testUUID);

			JobQueueModule queueModule = new JobQueueModule();
			assertEquals(testUUID, queueModule.provideAggregatorID());
		}
	}

	@Test
	void test_provideDpcAwsQueueConfiguration() throws NoSuchMethodException {
		assertSame(awsConfig, queueModule.provideDpcAwsQueueConfiguration());
	}

	@Test
	void test_provideCloudWatchAsyncClient() throws NoSuchMethodException {
		assertInstanceOf(CloudWatchAsyncClient.class, queueModule.provideCloudWatchAsyncClient());
	}

	@Test
	void test_provideCloudWatchReporter() throws NoSuchMethodException {
		CloudWatchAsyncClient client = queueModule.provideCloudWatchAsyncClient();
		assertInstanceOf(CloudWatchReporter.class, queueModule.provideCloudWatchReporter(metricRegistry, client));
	}

	@Test
	void test_provideSlf4jReporter() throws NoSuchMethodException {
		when(awsConfig.getQueueSizeMetricName()).thenReturn("metricName");
		assertInstanceOf(Slf4jReporter.class, queueModule.provideSlf4jReporter(metricRegistry));
	}

	@Test
	void test_provideScheduledReporter_Returns_CloudWatch_Reporter() throws NoSuchMethodException {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(true);
		CloudWatchAsyncClient client = queueModule.provideCloudWatchAsyncClient();
		assertInstanceOf(CloudWatchReporter.class, queueModule.provideScheduledReporter(metricRegistry, client));
	}

	@Test
	void test_provideScheduledReporter_Returns_Slf4j_Reporter() throws NoSuchMethodException {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(false);
		CloudWatchAsyncClient client = queueModule.provideCloudWatchAsyncClient();
		assertInstanceOf(Slf4jReporter.class, queueModule.provideScheduledReporter(metricRegistry, client));
	}

	@Test
	void test_Can_Configure_AWS_Queue() {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(true);
		final Injector injector = Guice.createInjector(queueModule, new MockModule<MockConfig>());
		assertInstanceOf(AwsDistributedBatchQueue.class, injector.getInstance(IJobQueue.class));
	}

	@Test
	void test_Can_Configure_Distributed_Queue() {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(false);
		final Injector injector = Guice.createInjector(queueModule, new MockModule<MockConfig>());
		assertInstanceOf(DistributedBatchQueue.class, injector.getInstance(IJobQueue.class));
	}

	// Dummy configuration class since we can't pull in the real DPCAggregationConfig
	private class MockConfig extends Configuration implements DPCQueueConfig {
		@Override
		public int getPollingFrequency() {
			return 0;
		}

		@Override
		public DPCAwsQueueConfiguration getDpcAwsQueueConfiguration() {
			return awsConfig;
		}
	}

	// Dummy Module that implements the providers we need to build our queues.
	private class MockModule <T extends Configuration & DPCQueueConfig> extends DropwizardAwareModule<T> {
		@Provides
		@JobTimeout
		public int provideJobTimeoutInSeconds() {
			return 100;
		}

		@Provides
		@ExportPath
		public String provideExportPath() {
			return "/path";
		}

		@Provides
		@Singleton
		DPCQueueManagedSessionFactory getSessionFactory() {
			SessionFactory factory = mock(SessionFactory.class);
			return new DPCQueueManagedSessionFactory(factory);
		}
	}
}
