package gov.cms.dpc.queue.config;

import javax.validation.constraints.NotNull;

public class DPCAwsQueueConfiguration {

	@NotNull
	private boolean emitAwsMetrics;

	@NotNull
	private String awsRegion;

	@NotNull
	private int awsReportingInterval;

	@NotNull
	private String environment;

	@NotNull
	private String awsNamespace;

	@NotNull
	private String queueSizeMetricName;

	public boolean getEmitAwsMetrics() { return emitAwsMetrics; }
	public String getAwsRegion() { return awsRegion; }
	public int getAwsReportingInterval() { return awsReportingInterval; }
	public String getEnvironment() { return environment; }
	public String getAwsNamespace() { return awsNamespace; }
	public String getQueueSizeMetricName() { return queueSizeMetricName; }
}
