package gov.cms.dpc.queue.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
@DisplayName("AWS Queue access")


class DPCAwsQueueConfigurationUnitTest {
	@Test
@DisplayName("Verify AWS queue setters and getters 🥳")

	void testSettersAndGetters() {
		DPCAwsQueueConfiguration config = new DPCAwsQueueConfiguration();

		config.setEmitAwsMetrics(true);
		assertTrue(config.getEmitAwsMetrics());

		config.setAwsRegion("region");
		assertEquals("region", config.getAwsRegion());

		config.setAwsSizeReportingInterval(60);
		assertEquals(60, config.getAwsSizeReportingInterval());

		config.setAwsAgeReportingInterval(60);
		assertEquals(60, config.getAwsAgeReportingInterval());

		config.setEnvironment("env");
		assertEquals("env", config.getEnvironment());

		config.setAwsNameSpace("namespace");
		assertEquals("namespace", config.getAwsNamespace());

		config.setQueueSizeMetricName("size");
		assertEquals("size", config.getQueueSizeMetricName());

		config.setQueueAgeMetricName("age");
		assertEquals("age", config.getQueueAgeMetricName());
	}
}
