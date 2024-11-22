package gov.cms.dpc.common.hibernate.queue;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import org.hibernate.SessionFactory;

/**
 * {@link Managed} interface that wraps a Hibernate {@link SessionFactory} and ensures that it is shutdown correctly when the service exits.
 * This is necessary because we manually inject the SessionFactory into various classes and thus we take ownership of its lifecycle.
 */
public class DPCQueueManagedSessionFactory implements Managed {

    private final SessionFactory sessionFactory;

    @Inject
    public DPCQueueManagedSessionFactory(SessionFactory factory) {
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
