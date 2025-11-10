package gov.cms.dpc.fhir.configuration;

public class ConnectionPoolConfiguration {
	private Integer poolMaxPerRoute;
	private Integer poolMaxTotal;

	public ConnectionPoolConfiguration() {
		// Needed for Jackson
	}

	public Integer getPoolMaxPerRoute() {
		return poolMaxPerRoute;
	}
	public void setPoolMaxPerRoute(Integer poolMaxPerRoute) {
		this.poolMaxPerRoute = poolMaxPerRoute;
	}

	public Integer getPoolMaxTotal() {
		return poolMaxTotal;
	}
	public void setPoolMaxTotal(Integer poolMaxTotal) {
		this.poolMaxTotal = poolMaxTotal;
	}
}
