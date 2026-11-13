package gov.cms.dpc.oneecosystem;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import gov.cms.dpc.oneecosystem.client.MockOneEcosystemClient;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClientImpl;
import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import gov.cms.dpc.oneecosystem.config.OneEcosystemBundleConfiguration;
import io.dropwizard.core.Configuration;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import java.nio.file.Path;

/**
 * Guice module for building and injecting the {@link OneEcosystemClient}.
 */
public class OneEcosystemClientModule<T extends Configuration & OneEcosystemBundleConfiguration> extends DropwizardAwareModule<T> {

    @Provides
    @Singleton
    public OneEcosystemClient provideOneEcosystemClient(@Named("r4") FhirContext fhirContext) {
        OneEcosystemClientConfiguration clientConfiguration = configuration().getOneEcosystemClientConfiguration();
        Path exportPath = Path.of(configuration().getExportPath());
        return clientConfiguration.isUseMock()
            ? new MockOneEcosystemClient(exportPath)
            : new OneEcosystemClientImpl(clientConfiguration, fhirContext);
    }
}
