package gov.cms.dpc.aws.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockAwsClient implements AwsClient {
	private static final Logger logger = LoggerFactory.getLogger(MockAwsClient.class);

	@Override
	public void emitJobBatchQueueSize(int queueSize) {
		// We're not connected to AWS, so don't emit anything.
		logger.debug("Mocking emit job batch queue size metric");
		return;
	}
}
