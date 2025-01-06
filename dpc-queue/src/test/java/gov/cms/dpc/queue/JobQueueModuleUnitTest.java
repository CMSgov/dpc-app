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
import jakarta.inject.Singleton;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.regions.Region;

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
		when(awsConfig.getQueueSizeMetricName()).thenReturn("sizeMetricName");
		when(awsConfig.getQueueAgeMetricName()).thenReturn("sizeAgeName");
		when(awsConfig.getAwsNamespace()).thenReturn("namespace");
		when(awsConfig.getAwsAgeReportingInterval()).thenReturn(15);
		when(awsConfig.getAwsSizeReportingInterval()).thenReturn(15);
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
	void test_provideAgeReporter() throws NoSuchMethodException {
		assertInstanceOf(Slf4jReporter.class, queueModule.provideAgeScheduledReporter(metricRegistry));
	}

	@Test
	void test_provideSizeReporter_emitting_to_aws() throws NoSuchMethodException {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(true);
		assertInstanceOf(CloudWatchReporter.class, queueModule.provideSizeScheduledReporter(metricRegistry));
	}

	@Test
	void test_provideSizeReporter_not_emitting_to_aws() throws NoSuchMethodException {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(false);
		assertInstanceOf(Slf4jReporter.class, queueModule.provideSizeScheduledReporter(metricRegistry));
	}

	@Test
	void test_Can_Configure_AWS_Queue() {
		when(awsConfig.getEmitAwsMetrics()).thenReturn(true);
		final Injector injector = Guice.createInjector(queueModule, new MockModule<MockConfig>());
		assertInstanceOf(AwsDistributedBatchQueue.class, injector.getInstance(IJobQueue.class));
	}

	@Test
	void test_Can_Configure_Distributed_Queue() {
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(null);
		final Injector injector = Guice.createInjector(queueModule, new MockModule<MockConfig>());
		IJobQueue queue = injector.getInstance(IJobQueue.class);

		// Can't use assertInstanceOf, because AwsDistributedBatchQueue is an DistributedBatchQueue
		assertEquals(DistributedBatchQueue.class, queue.getClass());
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
