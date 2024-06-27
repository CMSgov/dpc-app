package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import gov.cms.dpc.queue.config.DPCQueueConfig;
import io.dropwizard.core.Configuration;
import io.github.azagniotov.metrics.reporter.cloudwatch.CloudWatchReporter;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobQueueModuleUnitTest {
	MockConfig mockConfig = spy(new MockConfig());

	DPCAwsQueueConfiguration awsQueueConfig = mock(DPCAwsQueueConfiguration.class);

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
		JobQueueModule queueModule = spy(JobQueueModule.class);
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		DPCAwsQueueConfiguration awsConfig = new DPCAwsQueueConfiguration();
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);

		assertSame(awsConfig, queueModule.provideDpcAwsQueueConfiguration());
	}

	@Test
	void test_provideCloudWatchAsyncClient() throws NoSuchMethodException {
		JobQueueModule queueModule = spy(JobQueueModule.class);
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);
		when(awsConfig.getAwsRegion()).thenReturn(Region.US_EAST_1.toString());

		assertInstanceOf(CloudWatchAsyncClient.class, queueModule.provideCloudWatchAsyncClient());
	}

	@Test
	void test_provideCloudWatchReporter() throws NoSuchMethodException {
		JobQueueModule queueModule = spy(JobQueueModule.class);
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);
		when(awsConfig.getAwsRegion()).thenReturn(Region.US_EAST_1.toString());

		MetricRegistry metricRegistry = new MetricRegistry();
		CloudWatchAsyncClient client = queueModule.provideCloudWatchAsyncClient();

		assertInstanceOf(CloudWatchReporter.class, queueModule.provideCloudWatchReporter(metricRegistry, client));
	}

	@Test
	void test_provideSlf4jReporter() throws NoSuchMethodException {
		JobQueueModule queueModule = spy(JobQueueModule.class);
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);
		when(awsConfig.getQueueSizeMetricName()).thenReturn("metricName");

		MetricRegistry metricRegistry = new MetricRegistry();
		assertInstanceOf(Slf4jReporter.class, queueModule.provideSlf4jReporter(metricRegistry));
	}

	@Test
	void test_provideScheduledReporter_Returns_CloudWatch_Reporter() throws NoSuchMethodException {
		JobQueueModule queueModule = spy(JobQueueModule.class);
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);
		when(awsConfig.getEmitAwsMetrics()).thenReturn(true);
		when(awsConfig.getAwsRegion()).thenReturn(Region.US_EAST_1.toString());

		MetricRegistry metricRegistry = new MetricRegistry();
		CloudWatchAsyncClient client = queueModule.provideCloudWatchAsyncClient();

		assertInstanceOf(CloudWatchReporter.class, queueModule.provideScheduledReporter(metricRegistry, client));
	}

	@Test
	void test_provideScheduledReporter_Returns_Slf4j_Reporter() throws NoSuchMethodException {
		JobQueueModule queueModule = spy(JobQueueModule.class);
		ReflectionSupport.invokeMethod(
			DropwizardAwareModule.class.getDeclaredMethod("configuration"),
			doReturn(mockConfig).when(queueModule)
		);

		DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);
		when(mockConfig.getDpcAwsQueueConfiguration()).thenReturn(awsConfig);
		when(awsConfig.getEmitAwsMetrics()).thenReturn(false);
		when(awsConfig.getAwsRegion()).thenReturn(Region.US_EAST_1.toString());

		MetricRegistry metricRegistry = new MetricRegistry();
		CloudWatchAsyncClient client = queueModule.provideCloudWatchAsyncClient();

		assertInstanceOf(Slf4jReporter.class, queueModule.provideScheduledReporter(metricRegistry, client));
	}

	// Dummy configuration class since we can't pull in the real DPCAggregationConfig
	private class MockConfig extends Configuration implements DPCQueueConfig {
		@Override
		public int getPollingFrequency() {
			return 0;
		}

		@Override
		public DPCAwsQueueConfiguration getDpcAwsQueueConfiguration() {
			return null;
		}
	}
}
