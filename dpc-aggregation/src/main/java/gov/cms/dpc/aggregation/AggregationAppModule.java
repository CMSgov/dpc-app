package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.health.BlueButtonHealthCheck;
import gov.cms.dpc.aggregation.health.JobQueueHealthCheck;
import gov.cms.dpc.common.annotations.AdditionalPaths;
import gov.cms.dpc.common.hibernate.DPCHibernateBundle;

import javax.inject.Singleton;
import java.util.List;

public class AggregationAppModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    AggregationAppModule() {

    }

    @Override
    public void configure(Binder binder) {
        binder.requestStaticInjection(DPCHibernateBundle.class);
        binder.bind(AggregationEngine.class);
        binder.bind(Aggregation.class).asEagerSingleton();

        // Healthchecks
        binder.bind(JobQueueHealthCheck.class);
        binder.bind(BlueButtonHealthCheck.class);
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    public Config provideConfig() {
        return getConfiguration().getConfig();
    }

    @Provides
    @AdditionalPaths
    public List<String> provideAdditionalPaths() {
        return List.of("gov.cms.dpc.queue.models");
    }
}
