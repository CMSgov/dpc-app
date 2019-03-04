package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;

import javax.inject.Singleton;

public class AggregationModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    @Override
    public void configure(Binder binder) {

    }

    @Provides
    public Config provideConfig() {
        return getConfiguration().getConfig();
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }
}
