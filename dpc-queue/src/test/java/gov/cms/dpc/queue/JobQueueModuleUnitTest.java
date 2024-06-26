package gov.cms.dpc.queue;

import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import io.dropwizard.core.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JobQueueModuleUnitTest {
	private Configuration config = mock(Configuration.class);
	private DPCAwsQueueConfiguration awsConfig = mock(DPCAwsQueueConfiguration.class);

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
