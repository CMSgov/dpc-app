package gov.cms.dpc.aws;

import com.google.inject.Provides;
import gov.cms.dpc.aws.client.AwsClient;
import gov.cms.dpc.aws.client.AwsClientImpl;
import gov.cms.dpc.aws.client.MockAwsClient;
import gov.cms.dpc.aws.config.AwsClientBundleConfiguration;
import gov.cms.dpc.aws.config.AwsClientConfiguration;
import io.dropwizard.core.Configuration;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * Guice model for building and injecting the {@link gov.cms.dpc.aws.client.AwsClient}
 *
 * @param <T> DropWizard configuration class that implements {@link AwsClientBundleConfiguration}
 */
public class AwsClientModule <T extends Configuration & AwsClientBundleConfiguration> extends DropwizardAwareModule<T> {
	private AwsClientConfiguration awsClientConfiguration;

	public AwsClientModule() { this.awsClientConfiguration = null; }
	public AwsClientModule(AwsClientConfiguration awsConfig) { this.awsClientConfiguration = awsConfig; }

	@Override
	public void configure() {
		if (awsClientConfiguration == null) {
			awsClientConfiguration = configuration().getAwsClientConfiguration();
		}
	}

	@Provides
	public AwsClient provideAwsClient() {
		return awsClientConfiguration.isUseAwsMock() ? new MockAwsClient() : new AwsClientImpl();
	}
}
