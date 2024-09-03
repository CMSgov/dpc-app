package gov.cms.dpc.fhir.configuration;

/**
 * Used to load a fhir client's timeouts from an application's config file.
 */
public class TimeoutConfiguration {

	private Integer connectionTimeout;
	private Integer socketTimeout;
	private Integer requestTimeout;

	public TimeoutConfiguration() {
		// Jackson required
	}

	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Integer getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public Integer getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Integer requestTimeout) {
		this.requestTimeout = requestTimeout;
	}
}
