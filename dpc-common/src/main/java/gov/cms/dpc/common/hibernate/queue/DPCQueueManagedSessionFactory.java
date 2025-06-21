package gov.cms.dpc.common.hibernate.queue;

import gov.cms.dpc.common.utils.CurrentEngineState;
import io.dropwizard.lifecycle.Managed;
import jakarta.inject.Inject;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Managed} interface that wraps a Hibernate {@link SessionFactory} and ensures that it is shutdown correctly when the service exits.
 * This is necessary because we manually inject the SessionFactory into various classes and thus we take ownership of its lifecycle.
 */
public class DPCQueueManagedSessionFactory implements Managed {
    private static final Logger logger = LoggerFactory.getLogger(DPCQueueManagedSessionFactory.class);

    private final SessionFactory sessionFactory;
    private final CurrentEngineState engineState;

    @Inject
    public DPCQueueManagedSessionFactory(SessionFactory factory, CurrentEngineState engineState) {
        this.sessionFactory = factory;
        this.engineState = engineState;
    }

    @Override
    public void start() {
        // Not used
    }

    @Override
    public void stop() throws InterruptedException {
        logger.debug("Closing dpc-queue session factory");

        // Wait for the last batch to finish in aggregation, then shut down.
        logger.debug("Waiting for engine to stop");
        synchronized (engineState) {
            while(engineState.getState() != CurrentEngineState.States.STOPPED) {
                try {
                    System.out.println("Waiting for engine to stop");
                    engineState.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupt caught in manager");
                    this.sessionFactory.close();
                    throw e;
                }
            }
        }

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
