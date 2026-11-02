package gov.cms.dpc.oneecosystem;

import com.google.inject.Provides;
import gov.cms.dpc.oneecosystem.client.MockOneEcosystemClient;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
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
    public OneEcosystemClient provideOneEcosystemClient() {
        Path exportPath = Path.of(configuration().getExportPath());
        // Replace the non-mock client as part of DPC-5783
        // add configuration().getOneEcosystemClientConfiguration()
        // check clientConfiguration.isUseMock()
        return new MockOneEcosystemClient(exportPath);
    }
}
