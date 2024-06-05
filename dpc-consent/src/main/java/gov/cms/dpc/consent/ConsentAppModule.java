package gov.cms.dpc.consent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import gov.cms.dpc.common.health.FhirMetaDataHealthCheck;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.consent.resources.ConsentResource;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import javax.inject.Singleton;

class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure() {
        Binder binder = binder();
        binder.bind(ConsentResource.class);
        binder.bind(ConsentDAO.class);
        binder.bind(FhirMetaDataHealthCheck.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.configuration().getSuppressionFileDir();
    }

    @Provides
    @Named("fhirReferenceURL")
    public String provideFhirReferenceURL() { return configuration().getFhirReferenceURL(); }

    @Provides
    @Singleton
    @Named("localFhirClient")
    public IGenericClient provideLocalFhirClient(FhirContext ctx) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(provideFhirReferenceURL());
    }

    @Provides
    @Named("jerseyStarted")
    public boolean jerseyStarted() {
        return Boolean.TRUE.equals(environment().jersey().getProperty("Running"));
    }
}
