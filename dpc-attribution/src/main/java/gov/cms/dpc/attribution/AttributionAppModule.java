package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.attribution.resources.v1.*;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.health.FhirMetaDataHealthCheck;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import javax.inject.Singleton;
import java.time.Duration;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    @Override
    public void configure() {
        Binder binder = binder();

        // Resources
        binder.bind(V1AttributionResource.class);
        binder.bind(EndpointResource.class);
        binder.bind(PatientResource.class);
        binder.bind(PractitionerResource.class);
        binder.bind(GroupResource.class);
        binder.bind(OrganizationResource.class);

        // DAOs
        binder.bind(EndpointDAO.class);
        binder.bind(OrganizationDAO.class);
        binder.bind(PatientDAO.class);
        binder.bind(ProviderDAO.class);
        binder.bind(RosterDAO.class);
        binder.bind(RelationshipDAO.class);

        // Tasks
        binder.bind(TruncateDatabase.class);

        // Services

        // Healthchecks
        // Additional health-checks can be added here
        // By default, Dropwizard adds a check for Hibernate and each additional database (e.g. auth, queue, etc)
        binder.bind(FhirMetaDataHealthCheck.class);
    }

    @Provides
    Duration provideExpiration(DPCAttributionConfiguration config) {
        return config.getExpirationThreshold();
    }

    @Provides
    Settings provideSettings() {
        return new Settings().withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
    }

    @Provides
    // We can suppress this because the SessionFactory is managed
    @SuppressWarnings("CloseableProvides")
    SessionFactory provideSessionFactory(DPCManagedSessionFactory factory) {
        return factory.getSessionFactory();
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
}
