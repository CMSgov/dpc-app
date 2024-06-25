package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import io.github.azagniotov.metrics.reporter.cloudwatch.DimensionedName;

import java.util.concurrent.TimeUnit;

public class AwsDistributedBatchQueue extends DistributedBatchQueue {
	public AwsDistributedBatchQueue(
		DPCQueueManagedSessionFactory factory,
		@QueueBatchSize int batchSize,
		MetricRegistry metricRegistry,
		ScheduledReporter reporter,
		DPCAwsQueueConfiguration awsConfig
	) {
		super(factory, batchSize, metricRegistry);

		DimensionedName queueSize = DimensionedName
			.withName(awsConfig.getQueueSizeMetricName())
			.withDimension("environment", awsConfig.getEnvironment())
			.build();

		MetricMaker metricMaker = new MetricMaker(metricRegistry, AwsDistributedBatchQueue.class);
		metricMaker.registerCachedGauge(queueSize.encode(), this::queueSize);

		reporter.start(awsConfig.getAwsReportingInterval(), TimeUnit.SECONDS);
	}
}
