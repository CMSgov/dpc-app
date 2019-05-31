package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.health.RosterEngineHealthCheck;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.RelationshipDAO;
import gov.cms.dpc.attribution.jdbi.RosterEngine;
import gov.cms.dpc.attribution.resources.v1.GroupResource;
import gov.cms.dpc.attribution.resources.v1.V1AttributionResource;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.hibernate.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;

import java.time.Duration;

@SuppressWarnings("rawtypes") // Until we merge DPC-104
class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    AttributionAppModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ProviderDAO.class);
        binder.bind(AttributionEngine.class).to(RosterEngine.class);
        binder.bind(V1AttributionResource.class);
        binder.bind(TruncateDatabase.class);

        // Healthchecks
        binder.bind(RosterEngineHealthCheck.class);
    }

    /**
     * Manually construct and provide the {@link GroupResource}.
     * We need to do this because the resource is managed by Guice, which means the {@link io.dropwizard.hibernate.UnitOfWork} annotations do not work correctly.
     * This wraps the resource in the appropriate database transaction handling context.
     *
     * @param hibernateModule - {@link DPCHibernateBundle} hibernate module to get session from
     * @param engine          - {@link AttributionEngine} first constructor parameter (provided by Guice) to resource
     * @return - {@link GroupResource} with injected database session
     */
    @Provides
    GroupResource provideAttributionResource(DPCHibernateBundle hibernateModule, AttributionEngine engine) {
        return new UnitOfWorkAwareProxyFactory(hibernateModule)
                .create(GroupResource.class, AttributionEngine.class, engine);
    }

    @Provides
    RelationshipDAO provideRelationshipDAO(DPCHibernateBundle hibernateModule, DPCManagedSessionFactory factory) {
        return new UnitOfWorkAwareProxyFactory(hibernateModule)
                .create(RelationshipDAO.class, SessionFactory.class, factory);
    }

    @Provides
    Duration provideExpiration(DPCAttributionConfiguration config) {
        return config.getExpirationThreshold();
    }

    @Provides
    Settings provideSettings() {
        return new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    }
}
