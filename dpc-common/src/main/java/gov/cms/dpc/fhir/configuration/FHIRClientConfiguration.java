package gov.cms.dpc.fhir.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Used to load a fhir client's time-outs and base url from an application's config file.
 */
public class FHIRClientConfiguration {
	@NotEmpty
	private String serverBaseUrl;

	@Valid
	@NotNull
	@JsonProperty("timeouts")
	private TimeoutConfiguration timeouts = new TimeoutConfiguration();

	public TimeoutConfiguration getTimeouts() {
		return timeouts;
	}
	public void setTimeouts(TimeoutConfiguration timeouts) { this.timeouts = timeouts; }

	public String getServerBaseUrl() { return serverBaseUrl; }
	public void setServerBaseUrl(String serverBaseUrl) {
		this.serverBaseUrl = serverBaseUrl;
	}
}
