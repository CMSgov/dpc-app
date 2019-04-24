package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.RelationshipDAO;
import gov.cms.dpc.attribution.resources.v1.GroupResource;
import gov.cms.dpc.attribution.resources.v1.V1AttributionResource;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.hibernate.DPCHibernateBundle;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.hibernate.SessionFactory;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Duration;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(AttributionAppModule.class);


    AttributionAppModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ProviderDAO.class);
        binder.bind(AttributionEngine.class).to(ProviderDAO.class);
        binder.bind(V1AttributionResource.class);
        binder.bind(TruncateDatabase.class);
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
    RelationshipDAO provideRelationshipDAO(DPCHibernateBundle hibernateModule, SessionFactory factory) {
        return new UnitOfWorkAwareProxyFactory(hibernateModule)
                .create(RelationshipDAO.class, SessionFactory.class, factory);
    }

    @Provides
    Duration provideExpiration(DPCAttributionConfiguration config) {
        return config.getExpirationThreshold();
    }

    @Provides
    DSLContext provideDSL(DPCAttributionConfiguration config) {
        final DataSourceFactory factory = config.getDatabase();
        final ManagedDataSource dataSource = factory.build(getEnvironment().metrics(), "tested-things");
        final Settings settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);

        try {
            return DSL.using(dataSource.getConnection(), settings);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
