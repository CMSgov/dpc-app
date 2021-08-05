package gov.cms.dpc.bluebutton.clientV2;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestModule extends AbstractModule {

    TestModule() {
        // Not used
    }

    @Override
    protected void configure() {
        // Not used
    }

    @Provides
    Config provideTestConfig() {
        return ConfigFactory.load("test.application.conf").getConfig("dpc.aggregation");
    }

    @Named("fhirContextR4")
    @Provides
    FhirContext providesFhirR4Context() {
        return FhirContext.forR4();
    }
}
