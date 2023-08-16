package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.consent.resources.ConsentResource;

class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(ConsentResource.class);
        binder.bind(ConsentDAO.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return getConfiguration().getSuppressionFileDir();
    }

    @Provides
    @Named("fhirReferenceURL")
    public String provideFhirReferenceURL() { return getConfiguration().getFhirReferenceURL(); }
}
