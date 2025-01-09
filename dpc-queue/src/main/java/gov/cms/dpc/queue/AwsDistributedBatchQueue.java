package gov.cms.dpc.queue;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import io.github.azagniotov.metrics.reporter.cloudwatch.DimensionedName;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.concurrent.TimeUnit;

/**
 * Extension of {@link DistributedBatchQueue} that emits its current queue size as a metric to AWS Cloudwatch.
 */
public class AwsDistributedBatchQueue extends DistributedBatchQueue {
	@Inject
	public AwsDistributedBatchQueue(
		DPCQueueManagedSessionFactory factory,
		@QueueBatchSize int batchSize,
		MetricRegistry metricRegistry,
		@Named("QueueAge") ScheduledReporter ageReporter,
		@Named("QueueSize") ScheduledReporter sizeReporter,
		DPCAwsQueueConfiguration awsConfig
	) {
		super(factory, batchSize, metricRegistry);

		// Setup queue size metric
		DimensionedName queueSizeName = DimensionedName
			.withName(awsConfig.getQueueSizeMetricName())
			.withDimension("environment", awsConfig.getEnvironment())
			.build();
		metricRegistry.register(
			queueSizeName.encode(),
			(Gauge<Long>) this::queueSize
		);

		// Setup queue age metric
		DimensionedName queueAgeName = DimensionedName
			.withName(awsConfig.getQueueAgeMetricName())
			.withDimension("environment", awsConfig.getEnvironment())
			.build();
		metricRegistry.register(
			queueAgeName.encode(),
			(Gauge<Double>) this::queueAge
		);

		ageReporter.start(awsConfig.getAwsAgeReportingInterval(), TimeUnit.SECONDS);
		sizeReporter.start(awsConfig.getAwsSizeReportingInterval(), TimeUnit.SECONDS);
	}
}
