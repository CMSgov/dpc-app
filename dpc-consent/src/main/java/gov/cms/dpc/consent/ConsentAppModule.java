package gov.cms.dpc.consent;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.consent.resources.ConsentResource;

import javax.inject.Singleton;

class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(ConsentResource.class);
        binder.bind(ConsentDAO.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.getConfiguration().getSuppressionFileDir();
    }

    @Provides
    @Named("fhirReferenceURL")
    public String provideFhirReferenceURL() { return getConfiguration().getFhirReferenceURL(); }

    @Provides
    @Named("consentOrganizationURL")
    public String provideConsentOrganizationURL() { return getConfiguration().getConsentOrganizationURL(); }

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
