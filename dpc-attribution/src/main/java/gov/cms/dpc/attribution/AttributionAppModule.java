package gov.cms.dpc.attribution;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.RelationshipDAO;
import gov.cms.dpc.attribution.resources.v1.GroupResource;
import gov.cms.dpc.attribution.resources.v1.V1AttributionResource;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.hibernate.SessionFactory;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Entity;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(AttributionAppModule.class);


    AttributionAppModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(AttributionHibernateModule.class).in(Scopes.SINGLETON);
        binder.bind(ProviderDAO.class);
        binder.bind(AttributionEngine.class).to(ProviderDAO.class);
        binder.bind(V1AttributionResource.class);
    }

    @Provides
    @Singleton
    SessionFactory getSessionFactory(AttributionHibernateModule hibernate) {
        // This is necessary because the session factory doesn't load on its own.
        // I'm really not sure how to fix this, I think it's due to the interaction with the Proxy Factory
        final DPCAttributionConfiguration configuration = getConfiguration();
        try {
            hibernate.run(configuration, getEnvironment());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hibernate.getSessionFactory();
    }

    /**
     * Manually construct and provide the {@link GroupResource}.
     * We need to do this because the resource is managed by Guice, which means the {@link io.dropwizard.hibernate.UnitOfWork} annotations do not work correctly.
     * This wraps the resource in the appropriate database transaction handling context.
     *
     * @param hibernateModule - {@link AttributionHibernateModule} hibernate module to get session from
     * @param engine          - {@link AttributionEngine} first constructor parameter (provided by Guice) to resource
     * @return - {@link GroupResource} with injected database session
     */
    @Provides
    GroupResource provideAttributionResource(AttributionHibernateModule hibernateModule, AttributionEngine engine) {
        return new UnitOfWorkAwareProxyFactory(hibernateModule)
                .create(GroupResource.class, AttributionEngine.class, engine);
    }

    @Provides
    RelationshipDAO provideRelationshipDAO(AttributionHibernateModule hibernateModule, SessionFactory factory) {
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

    public static class AttributionHibernateModule extends HibernateBundle<DPCAttributionConfiguration> implements ConfiguredBundle<DPCAttributionConfiguration> {

        private static final Logger logger = LoggerFactory.getLogger(AttributionHibernateModule.class);
        public static String PREFIX_STRING = "gov.cms.dpc.common.entities";

        @Inject
        public AttributionHibernateModule() {
            super(applicationEntities(), new SessionFactoryFactory());
        }

        private static ImmutableList<Class<?>> applicationEntities() {
            logger.info("Scanning {} for Hibernate entities", PREFIX_STRING);

            final Reflections reflections = new Reflections(PREFIX_STRING);
            final Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
            logger.info("Found {} Hibernate entities", entities.getClass());
            if (logger.isDebugEnabled()) {
                entities.forEach((entity) -> logger.debug("Registered {}.", entity.getName()));
            }
            return ImmutableList.copyOf(entities);
        }

        @Override
        public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
            return configuration.getDatabase();
        }
    }
}
