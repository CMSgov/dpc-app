package gov.cms.dpc.common.hibernate.attribution;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

import static gov.cms.dpc.common.hibernate.EntityScanner.applicationEntities;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCStu3HibernateBundle#PREFIX_STRING} for a list of annotated entities.
 *
 * @param <T> - Configuration class type
 */
public class DPCStu3HibernateBundle<T extends Configuration & IDPCDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    public static final String PREFIX_STRING = "gov.cms.dpc.common.entities.stu3";
    public static final String BUNDLE_NAME = "stu3";

    @Inject
    public DPCStu3HibernateBundle() {
        this(Collections.emptyList());
    }

    public DPCStu3HibernateBundle(List<String> additionalPaths) {
        super(applicationEntities(PREFIX_STRING, additionalPaths), new SessionFactoryFactory());
    }

    @Override
    protected String name() {
        return BUNDLE_NAME;
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getDatabase();
    }
}
