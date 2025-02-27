package gov.cms.dpc.common.hibernate.queue;

import com.google.inject.Inject;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;

import javax.inject.Singleton;

import java.util.Collections;

import static gov.cms.dpc.common.hibernate.EntityScanner.applicationEntities;

/**
 * Custom Hibernate bundle, which allows us to inject the {@link org.hibernate.SessionFactory} into non-resource types.
 * By default, the bundle scans the {@link DPCQueueHibernateBundle#PREFIX_STRING} for a list of annotated entities.
 *
 * @param <T> - Configuration class type
 */
@Singleton
public class DPCQueueHibernateBundle<T extends Configuration & IDPCQueueDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    public static String PREFIX_STRING = "gov.cms.dpc.queue.models";

    @Inject
    public DPCQueueHibernateBundle() {
        super(applicationEntities(Collections.singletonList(PREFIX_STRING)), new SessionFactoryFactory());
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
