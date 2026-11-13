package gov.cms.dpc.oneecosystem.client;

import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.validation.BaseValidator;

import java.io.IOException;

final class OneEcosystemTestHelpers {

    private OneEcosystemTestHelpers() {
    }

    static OneEcosystemClientConfiguration loadConfig(String resource) throws IOException, ConfigurationException {
        var source = new SubstitutingSourceProvider(
                new ResourceConfigurationSourceProvider(), new EnvironmentVariableSubstitutor());
        var factory = new DefaultConfigurationFactoryFactory<OneEcosystemClientConfiguration>()
                .create(OneEcosystemClientConfiguration.class, BaseValidator.newValidator(), Jackson.newObjectMapper(), "dw");
        return factory.build(source, resource);
    }
}
