package gov.cms.dpc.aws.client;

public interface AwsClient {
	/**
	 * Emits the size of the current job batch queue as an AWS CloudWatch custom metric.  If there's a problem a warning
	 * is logged, but no exception is thrown.
	 *
	 * @param queueSize The number of job batches waiting in the queue to be processed.
	 */
	void emitJobBatchQueueSize(int queueSize);
}
