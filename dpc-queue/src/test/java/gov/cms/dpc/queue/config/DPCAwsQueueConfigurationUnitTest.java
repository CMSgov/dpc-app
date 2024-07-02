package gov.cms.dpc.queue.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DPCAwsQueueConfigurationUnitTest {
	@Test
	void testSettersAndGetters() {
		DPCAwsQueueConfiguration config = new DPCAwsQueueConfiguration();

		config.setEmitAwsMetrics(true);
		assertTrue(config.getEmitAwsMetrics());

		config.setAwsRegion("region");
		assertEquals("region", config.getAwsRegion());

		config.setAwsReporitingInterval(60);
		assertEquals(60, config.getAwsReportingInterval());

		config.setEnvironment("env");
		assertEquals("env", config.getEnvironment());

		config.setAwsNameSpace("namespace");
		assertEquals("namespace", config.getAwsNamespace());

		config.setQueueSizeMetricName("metric");
		assertEquals("metric", config.getQueueSizeMetricName());
	}
}
