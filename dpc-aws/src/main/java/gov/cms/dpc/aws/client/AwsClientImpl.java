package gov.cms.dpc.aws.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AwsClientImpl implements AwsClient {
	private static final Logger logger = LoggerFactory.getLogger(AwsClientImpl.class);

	private final CloudWatchClient cloudWatchClient;

	public AwsClientImpl() {
		cloudWatchClient = CloudWatchClient.builder()
			.build();
	}

	@Override
	public void emitJobBatchQueueSize(int queueSize) {
		logger.debug("Emitting job batch queue size: {}", queueSize);

		try {
			Dimension dimension = Dimension.builder()
				.name("Environment")
				.value("local")
				.build();

			// Set an Instant object
			String time = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
			Instant instant = Instant.parse(time);

			MetricDatum datum = MetricDatum.builder()
				.metricName("JobBatchQueueSize")
				.unit(StandardUnit.COUNT)
				.value(Double.valueOf(queueSize))
				.timestamp(instant)
				.dimensions(dimension).build();

			PutMetricDataRequest request = PutMetricDataRequest.builder()
				.namespace("DPC")
				.metricData(datum).build();

			cloudWatchClient.putMetricData(request);

		} catch (CloudWatchException e) {
			logger.error("Could not emit job batch queue size to CloudWatch: {}", e.getMessage());
		}
	}
}
