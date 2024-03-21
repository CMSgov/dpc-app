package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.consent.resources.ConsentResource;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure() {
        Binder binder = binder();
        binder.bind(ConsentResource.class);
        binder.bind(ConsentDAO.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.configuration().getSuppressionFileDir();
    }

    @Provides
    @Named("fhirReferenceURL")
    public String provideFhirReferenceURL() { return configuration().getFhirReferenceURL(); }
}
