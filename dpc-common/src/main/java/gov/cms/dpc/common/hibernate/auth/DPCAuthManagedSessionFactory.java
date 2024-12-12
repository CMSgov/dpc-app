package gov.cms.dpc.common.hibernate.auth;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Managed} interface that wraps a Hibernate {@link SessionFactory} and ensures that it is shutdown correctly when the service exits.
 * This is necessary because we manually inject the SessionFactory into various classes and thus we take ownership of its lifecycle.
 */
public class DPCAuthManagedSessionFactory implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(DPCAuthManagedSessionFactory.class);
    private final SessionFactory sessionFactory;

    @Inject
    public DPCAuthManagedSessionFactory(SessionFactory factory) {
        LOG.info("DPCAuthManagedSessionFactory here with SessionFactory " + factory);
        this.sessionFactory = factory;
    }

    @Override
    public void start() {
        // Not used
    }

    @Override
    public void stop() {
        this.sessionFactory.close();
    }

    /**
     * Get the underlying {@link SessionFactory} that this resource manages
     * The caller is responsible for cleaning up any generated {@link org.hibernate.Session} resources, but the application takes care of the factory.
     *
     * @return - {@link SessionFactory} managed by this resource
     */
    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

}
