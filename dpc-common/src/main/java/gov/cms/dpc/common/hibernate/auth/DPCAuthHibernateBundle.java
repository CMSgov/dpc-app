package gov.cms.dpc.common.hibernate.auth;

import com.google.inject.Inject;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;

import javax.inject.Singleton;

import java.util.Collections;
import java.util.List;

import static gov.cms.dpc.common.hibernate.EntityScanner.applicationEntities;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCAuthHibernateBundle#PREFIX_STRING} for a list of annotated entities.
 *
 * @param <T> - Configuration class type
 */
@Singleton
public class DPCAuthHibernateBundle<T extends Configuration & IDPCAuthDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    public static String PREFIX_STRING = "gov.cms.dpc.api.entities";

    @Inject
    public DPCAuthHibernateBundle() {
        super(applicationEntities(Collections.singletonList(PREFIX_STRING)), new SessionFactoryFactory());
    }

    public DPCAuthHibernateBundle(List<String> additionalPrefixes) {
        super(applicationEntities(PREFIX_STRING, additionalPrefixes), new SessionFactoryFactory());
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getAuthDatabase();
    }

    @Override
    protected String name() {
        return "hibernate.auth";
    }
}
