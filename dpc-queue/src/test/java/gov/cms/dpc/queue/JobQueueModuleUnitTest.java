package gov.cms.dpc.queue;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobQueueModuleUnitTest {
	@Test
	void testProvideBatchSize() {
		JobQueueModule queueModule = new JobQueueModule();
		assertEquals(100, queueModule.provideBatchSize());
	}

	@Test
	void testProvideAggregatorId() {
		UUID testUUID = UUID.randomUUID();

		try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
			uuid.when(UUID::randomUUID).thenReturn(testUUID);

			JobQueueModule queueModule = new JobQueueModule();
			assertEquals(testUUID, queueModule.provideAggregatorID());
		}
	}
}
