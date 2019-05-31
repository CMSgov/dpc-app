package gov.cms.dpc.common.hibernate;

import io.dropwizard.lifecycle.Managed;
import org.hibernate.SessionFactory;

/**
 * {@link Managed} interface that wraps a Hibernate {@link SessionFactory} and ensures that it is shutdown correctly when the service exits.
 * This is necessary because we manually inject the SessionFactory into various classes and thus we take ownership of its lifecycle.
 */
public class DPCManagedSessionFactory implements Managed {

    private final SessionFactory sessionFactory;

    public DPCManagedSessionFactory(SessionFactory factory) {
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

    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

}
