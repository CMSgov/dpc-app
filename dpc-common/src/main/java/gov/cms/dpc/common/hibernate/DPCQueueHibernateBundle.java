package gov.cms.dpc.common.hibernate;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCQueueHibernateBundle#PREFIX_STRING} for a list of annotated entities.
 *
 * @param <T> - Configuration class type
 */
@Singleton
public class DPCQueueHibernateBundle<T extends Configuration & IDPCQueueDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    private static final Logger logger = LoggerFactory.getLogger(DPCQueueHibernateBundle.class);
    public static String PREFIX_STRING = "gov.cms.dpc.queue.models";

    @Inject
    public DPCQueueHibernateBundle() {
        super(applicationEntities(), new SessionFactoryFactory());
    }

    private static ImmutableList<Class<?>> applicationEntities() {
        // Build a list of class paths to add
        List<String> paths = new ArrayList<>(List.of(PREFIX_STRING));

        final List<Class<?>> collect = paths
                .stream()
                .map(path -> {
                    logger.info("Scanning {} for Hibernate entities", path);
                    final Reflections reflections = new Reflections(path);
                    final Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
                    logger.info("Found {} Hibernate entities", entities.getClass());
                    if (logger.isDebugEnabled()) {
                        entities.forEach((entity) -> logger.debug("Registered {}.", entity.getName()));
                    }
                    return entities;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        return ImmutableList.copyOf(collect);
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getQueueDatabase();
    }

    @Override
    protected String name() {
        return "hibernate.queue";
    }
}
