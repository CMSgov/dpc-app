package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.consent.resources.BaseResource;
import gov.cms.dpc.consent.resources.ConsentResource;
import gov.cms.dpc.fhir.parameters.ProvenanceResourceValueFactory;
import jakarta.inject.Named;

class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure() {
        Binder binder = binder();
        binder.bind(ConsentResource.class);
        binder.bind(BaseResource.class);
        binder.bind(ConsentDAO.class);
        bind(ProvenanceResourceValueFactory.class).in(Scopes.SINGLETON);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.configuration().getSuppressionFileDir();
    }

    @Provides
    @Named("fhirReferenceURL")
    public String provideFhirReferenceURL() { return configuration().getFhirReferenceURL(); }
}
