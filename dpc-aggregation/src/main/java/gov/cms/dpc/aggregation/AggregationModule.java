package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.resources.v1.BeneficiaryResource;
import gov.cms.dpc.aggregation.resources.v1.V1AggregationResource;

import javax.inject.Singleton;

public class AggregationModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(AggregationEngine.class);
        binder.bind(V1AggregationResource.class);

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

    @Provides
    public BeneficiaryResource provideAggregationResource(AggregationEngine engine) {
        return new BeneficiaryResource(engine);
    }
}
