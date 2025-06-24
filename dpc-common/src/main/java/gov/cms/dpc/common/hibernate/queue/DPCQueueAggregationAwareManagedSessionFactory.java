package gov.cms.dpc.common.hibernate.queue;

import gov.cms.dpc.common.utils.CurrentEngineState;
import jakarta.inject.Inject;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DPCQueueManagedSessionFactory} that is aware of the current state of the dpc-aggregation
 * engine, and will wait to shut down its Hibernate session factory until while the engine is still running.
 */
public class DPCQueueAggregationAwareManagedSessionFactory extends DPCQueueManagedSessionFactory {
	private static final Logger logger = LoggerFactory.getLogger(DPCQueueAggregationAwareManagedSessionFactory.class);

	private final CurrentEngineState engineState;

	@Inject
	public DPCQueueAggregationAwareManagedSessionFactory(SessionFactory factory, CurrentEngineState engineState) {
		super(factory);
		this.engineState = engineState;
	}

	@Override
	public void stop() {
		logger.info("Waiting to close dpc-queue session factory");
		SessionFactory sessionFactory = this.getSessionFactory();

		// Wait for the last batch to finish in aggregation, then shut down.
		synchronized (engineState) {
			while(engineState.getState() != CurrentEngineState.States.STOPPED) {
				try {
					engineState.wait();
				} catch (InterruptedException e) {
					logger.info("Closing dpc-queue session factory");
					sessionFactory.close();
					Thread.currentThread().interrupt();
				}
			}
		}

		logger.info("Closing dpc-queue session factory");
		sessionFactory.close();
	}
}
