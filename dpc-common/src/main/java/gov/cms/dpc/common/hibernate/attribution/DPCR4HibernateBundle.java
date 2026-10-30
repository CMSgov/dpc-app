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
 * Hibernate bundle for R4 attribution entities, kept separate from the STU3 entity mappings.
 *
 * @param <T> - Configuration class type
 */
public class DPCR4HibernateBundle<T extends Configuration & IDPCDatabase> extends HibernateBundle<T> implements ConfiguredBundle<T> {

    public static final String PREFIX_STRING = "gov.cms.dpc.common.entities.r4";
    public static final String BUNDLE_NAME = "r4";

    @Inject
    public DPCR4HibernateBundle() {
        this(Collections.emptyList());
    }

    public DPCR4HibernateBundle(List<String> additionalPaths) {
        super(applicationEntities(PREFIX_STRING, additionalPaths), new SessionFactoryFactory());
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(T configuration) {
        return configuration.getDatabase();
    }

    @Override
    protected String name() {
        return BUNDLE_NAME;
    }
}
