package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.attribution.resources.v1.*;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import java.time.Duration;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    AttributionAppModule() {
        // Not used
    }

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
}
