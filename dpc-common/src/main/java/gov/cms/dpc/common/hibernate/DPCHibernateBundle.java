package gov.cms.dpc.common.hibernate;

import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.Entity;
import java.util.Set;

public class DPCHibernateBundle<T extends Configuration & IDPCDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    private static final Logger logger = LoggerFactory.getLogger(DPCHibernateModule.class);
    public static String PREFIX_STRING = "gov.cms.dpc.common.entities";

    @Inject
    public DPCHibernateBundle() {
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
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getDatabase();
    }
}
