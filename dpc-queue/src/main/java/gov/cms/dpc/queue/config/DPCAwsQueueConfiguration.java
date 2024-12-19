package gov.cms.dpc.queue.config;

import jakarta.validation.constraints.NotNull;

public class DPCAwsQueueConfiguration {

	@NotNull
	private boolean emitAwsMetrics;

	@NotNull
	private String awsRegion;

	@NotNull
	private int awsSizeReportingInterval;

	@NotNull
	private int awsAgeReportingInterval;

	@NotNull
	private String environment;

	@NotNull
	private String awsNamespace;

	@NotNull
	private String queueSizeMetricName;

	@NotNull
	private String queueAgeMetricName;

	public boolean getEmitAwsMetrics() {
		return emitAwsMetrics;
	}
	public DPCAwsQueueConfiguration setEmitAwsMetrics(boolean emitAwsMetrics) {
		this.emitAwsMetrics = emitAwsMetrics;
		return this;
	}

	public String getAwsRegion() {
		return awsRegion;
	}
	public DPCAwsQueueConfiguration setAwsRegion(String awsRegion) {
		this.awsRegion = awsRegion;
		return this;
	}

	public int getAwsSizeReportingInterval() {
		return awsSizeReportingInterval;
	}
	public DPCAwsQueueConfiguration setAwsSizeReportingInterval(int awsSizeReportingInterval) {
		this.awsSizeReportingInterval = awsSizeReportingInterval;
		return this;
	}

	public int getAwsAgeReportingInterval() {
		return awsAgeReportingInterval;
	}
	public DPCAwsQueueConfiguration setAwsAgeReportingInterval(int awsAgeReportingInterval) {
		this.awsAgeReportingInterval = awsAgeReportingInterval;
		return this;
	}

	public String getEnvironment() {
		return environment;
	}
	public DPCAwsQueueConfiguration setEnvironment(String environment) {
		this.environment = environment;
		return this;
	}

	public String getAwsNamespace() {
		return awsNamespace;
	}
	public DPCAwsQueueConfiguration setAwsNameSpace(String awsNamespace) {
		this.awsNamespace = awsNamespace;
		return this;
	}

	public String getQueueSizeMetricName() {
		return queueSizeMetricName;
	}
	public DPCAwsQueueConfiguration setQueueSizeMetricName(String queueSizeMetricName) {
		this.queueSizeMetricName = queueSizeMetricName;
		return this;
	}

	public String getQueueAgeMetricName() {
		return queueAgeMetricName;
	}
	public DPCAwsQueueConfiguration setQueueAgeMetricName(String queueAgeMetricName) {
		this.queueAgeMetricName = queueAgeMetricName;
		return this;
	}
}
