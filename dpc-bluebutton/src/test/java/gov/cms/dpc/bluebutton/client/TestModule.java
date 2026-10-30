package gov.cms.dpc.bluebutton.client;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.AbstractModule;
import com.google.inject.name.Named;
import com.google.inject.Provides;
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
    @Named("stu3")
    // Providing this here so we don't have to pull in the full FHIRModule for testing.
    FhirContext provideStu3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    Config provideTestConfig() {
        return ConfigFactory.load("test.application.conf").getConfig("dpc.aggregation");
    }
}
