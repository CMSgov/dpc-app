package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.consent.resources.BaseResource;
import gov.cms.dpc.consent.resources.ConsentResource;
import gov.cms.dpc.consent.tasks.TruncateDatabase;
import jakarta.inject.Named;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure() {
        Binder binder = binder();
        binder.bind(ConsentResource.class);
        binder.bind(BaseResource.class);
        binder.bind(ConsentDAO.class);

        // Tasks
        binder.bind(TruncateDatabase.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.configuration().getSuppressionFileDir();
    }

    @Provides
    @Named("fhirReferenceURL")
    public String provideFhirReferenceURL() { return configuration().getFhirReferenceURL(); }
}
