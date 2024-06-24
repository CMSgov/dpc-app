package gov.cms.dpc.aws.config;

public interface AwsClientBundleConfiguration {
	/**
	 * Get the config for the {@link AwsClientConfiguration}
	 *
	 * @return The {@link AwsClientConfiguration}
	 */
	AwsClientConfiguration getAwsClientConfiguration();
}
