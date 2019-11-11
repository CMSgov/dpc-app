package gov.cms.dpc.common.hibernate.consent;

import com.google.inject.Inject;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;

import javax.inject.Singleton;
import java.util.Collections;

import static gov.cms.dpc.common.hibernate.EntityScanner.applicationEntities;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCConsentHibernateBundle#PREFIX_STRING} for a list of annotated entities.
 *
 * @param <T> - Configuration class type
 */
@Singleton
public class DPCConsentHibernateBundle<T extends Configuration & IDPCConsentDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    public static String PREFIX_STRING = "gov.cms.dpc.consent.models";

    @Inject
    public DPCConsentHibernateBundle() {
        super(applicationEntities(Collections.singletonList(PREFIX_STRING)), new SessionFactoryFactory());
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getConsentDatabase();
    }

    @Override
    protected String name() {
        return "hibernate.consent";
    }
}
