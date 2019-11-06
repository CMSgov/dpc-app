package gov.cms.dpc.consent;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.fhir.hapi.ContextUtils;

import javax.inject.Singleton;

public class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(ConsentDAO.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.getConfiguration().getSuppressionFileDir();
    }

    @Provides
    @Singleton
    public MetricRegistry provideMetricRegistry() {
        return getEnvironment().metrics();
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }
}
