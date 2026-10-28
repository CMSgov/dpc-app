package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import gov.cms.dpc.attribution.jdbi.r4.OrganizationDAO;
import gov.cms.dpc.attribution.jdbi.r4.PatientDAO;
import gov.cms.dpc.attribution.jdbi.r4.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.r4.RelationshipDAO;
import gov.cms.dpc.attribution.jdbi.r4.RosterDAO;
import gov.cms.dpc.attribution.resources.r4.AttributionResource;
import gov.cms.dpc.attribution.resources.r4.BaseResource;
import gov.cms.dpc.attribution.resources.r4.OrganizationResource;
import gov.cms.dpc.attribution.resources.r4.PractitionerResource;
import gov.cms.dpc.attribution.resources.v1.*;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import java.time.Duration;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    @Override
    public void configure() {
        Binder binder = binder();

        // STU3 resources
        binder.bind(V1AttributionResource.class);
        binder.bind(PatientResource.class);
        binder.bind(gov.cms.dpc.attribution.resources.v1.PractitionerResource.class);
        binder.bind(GroupResource.class);
        binder.bind(gov.cms.dpc.attribution.resources.v1.OrganizationResource.class);
        binder.bind(gov.cms.dpc.attribution.resources.v1.BaseResource.class);

        // R4 resources
        binder.bind(BaseResource.class);
        binder.bind(AttributionResource.class);
        binder.bind(OrganizationResource.class);
        binder.bind(PractitionerResource.class);

        // STU3 DAOs
        binder.bind(gov.cms.dpc.attribution.jdbi.stu3.OrganizationDAO.class);
        binder.bind(gov.cms.dpc.attribution.jdbi.stu3.PatientDAO.class);
        binder.bind(gov.cms.dpc.attribution.jdbi.stu3.ProviderDAO.class);
        binder.bind(gov.cms.dpc.attribution.jdbi.stu3.RosterDAO.class);
        binder.bind(gov.cms.dpc.attribution.jdbi.stu3.RelationshipDAO.class);

        // R4 DAOs
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
    SessionFactory provideSessionFactory(@Named("stu3") DPCManagedSessionFactory factory) {
        return factory.getSessionFactory();
    }

    @Provides
    @Singleton
    @Named("queryChunkSize")
    int provideQueryChunkSize() { return configuration().getQueryChunkSize(); }

    @Provides
    @Singleton
    @Named("DbBatchSize")
    int provideDbBatchSize() { return configuration().getDbBatchSize(); }
}
