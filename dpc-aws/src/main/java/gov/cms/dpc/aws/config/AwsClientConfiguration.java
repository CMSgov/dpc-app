package gov.cms.dpc.aws.config;

import javax.validation.constraints.NotEmpty;

public class AwsClientConfiguration {
	private boolean useAwsMock = false;

	@NotEmpty
	private String environment;

	public boolean isUseAwsMock() {
		return useAwsMock;
	}
	public String getEnvironment() { return environment; }
}
