package gov.cms.dpc.attribution;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.attribution.resources.v1.*;
import gov.cms.dpc.attribution.service.DataService;
import gov.cms.dpc.attribution.service.LookBackService;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;

import javax.inject.Singleton;
import java.time.Duration;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    AttributionAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {
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
        binder.bind(DataService.class);
        binder.bind(LookBackService.class);

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

    @Provides
    @ExportPath
    public String provideExportPath() {
        return getConfiguration().getExportPath();
    }

    @Provides
    @Singleton
    public MetricRegistry provideMetricRegistry() {
        return getEnvironment().metrics();
    }
}
