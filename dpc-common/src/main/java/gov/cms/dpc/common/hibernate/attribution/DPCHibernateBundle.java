package gov.cms.dpc.common.hibernate.attribution;

import com.google.inject.Inject;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;

import java.util.Collections;
import java.util.List;

import static gov.cms.dpc.common.hibernate.EntityScanner.applicationEntities;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCHibernateBundle#PREFIX_STRING} for a list of annotated entities.
 *
 * @param <T> - Configuration class type
 */
public class DPCHibernateBundle<T extends Configuration & IDPCDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    public static String PREFIX_STRING = "gov.cms.dpc.common.entities";

    @Inject
    public DPCHibernateBundle() {
        this(Collections.emptyList());
    }

    public DPCHibernateBundle(List<String> additionalPaths) {
        super(applicationEntities(PREFIX_STRING, additionalPaths), new SessionFactoryFactory());
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getDatabase();
    }
}
