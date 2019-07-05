package gov.cms.dpc.common.hibernate;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import gov.cms.dpc.common.annotations.AdditionalPaths;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCHibernateBundle#PREFIX_STRING} for a list of annotated entities.
 * Additional paths can be added by injecting a {@link List} of {@link String} paths using the {@link AdditionalPaths}.
 * In order for this to work, the injector needs to request static binding using the {@link com.google.inject.Binder#requestStaticInjection(Class[])} method.
 *
 * @param <T> - Configuration class type
 */
public class DPCHibernateBundle<T extends Configuration & IDPCDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    private static final Logger logger = LoggerFactory.getLogger(DPCHibernateModule.class);
    public static String PREFIX_STRING = "gov.cms.dpc.common.entities";


    // We have to use a Guice specific annotation to get optional binding of additional class paths to scan
    @Inject(optional = true)
    @AdditionalPaths
    private static List<String> additionalPaths;

    private final DataSourceFactory factory;

    @Inject
    public DPCHibernateBundle(DataSourceFactory factory) {
        super(applicationEntities(), new SessionFactoryFactory());
        this.factory = factory;
    }

    private static ImmutableList<Class<?>> applicationEntities() {
        // Build a list of class paths to add
        List<String> paths = new ArrayList<>(List.of(PREFIX_STRING));
        if (additionalPaths != null) {
            paths.addAll(additionalPaths);
        }

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
        return this.factory;
    }
}
