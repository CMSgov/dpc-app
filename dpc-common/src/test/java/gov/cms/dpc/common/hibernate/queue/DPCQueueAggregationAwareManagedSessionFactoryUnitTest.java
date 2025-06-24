package gov.cms.dpc.common.hibernate.queue;

import gov.cms.dpc.common.utils.CurrentEngineState;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DPCQueueAggregationAwareManagedSessionFactoryUnitTest {
	@Test
	void testWaitToStop() throws InterruptedException, ExecutionException {
		SessionFactory sessionFactory = mock(SessionFactory.class);

		CurrentEngineState state = new CurrentEngineState();
		ExecutorService service = Executors.newFixedThreadPool(1);

		// New thread with a session factory that's waiting to stop
		Future<?> result = service.submit(() -> {
			new DPCQueueAggregationAwareManagedSessionFactory(sessionFactory, state).stop();
		});

		// Mark the engine as stopped and wait for session factory to finish stopping
		state.setState(CurrentEngineState.States.STOPPED);
		result.get();

		verify(sessionFactory).close();
	}
}
