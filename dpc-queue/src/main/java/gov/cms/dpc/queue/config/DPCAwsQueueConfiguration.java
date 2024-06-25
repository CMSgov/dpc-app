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
	public DPCAwsQueueConfiguration setEmitAwsMetrics(boolean emitAwsMetrics) {
		this.emitAwsMetrics = emitAwsMetrics;
		return this;
	}

	public String getAwsRegion() { return awsRegion; }
	public DPCAwsQueueConfiguration setAwsRegion(String awsRegion) {
		this.awsRegion = awsRegion;
		return this;
	}

	public int getAwsReportingInterval() { return awsReportingInterval; }
	public DPCAwsQueueConfiguration setAwsReporitingInterval(int awsReporitingInterval) {
		this.awsReportingInterval = awsReporitingInterval;
		return this;
	}

	public String getEnvironment() { return environment; }
	public DPCAwsQueueConfiguration setEnvironment(String environment) {
		this.environment = environment;
		return this;
	}

	public String getAwsNamespace() { return awsNamespace; }
	public DPCAwsQueueConfiguration setAwsNameSpace(String awsNamespace) {
		this.awsNamespace = awsNamespace;
		return this;
	}

	public String getQueueSizeMetricName() { return queueSizeMetricName; }
	public DPCAwsQueueConfiguration setQueueSizeMetricName(String queueSizeMetricName) {
		this.queueSizeMetricName = queueSizeMetricName;
		return this;
	}
}
