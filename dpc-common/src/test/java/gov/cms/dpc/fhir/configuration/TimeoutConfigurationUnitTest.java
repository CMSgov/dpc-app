package gov.cms.dpc.fhir.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test has no real practical value, but we need to get code coverage up to pass SonarQube checks on it.
 */
class TimeoutConfigurationUnitTest {
	private final TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration();
	@Test
	public void testSettersAndGetters() {
		int timeOut = 10;

		timeoutConfiguration.setConnectionTimeout(timeOut);
		timeoutConfiguration.setSocketTimeout(timeOut);
		timeoutConfiguration.setRequestTimeout(timeOut);
		assertEquals(timeOut, timeoutConfiguration.getConnectionTimeout());
		assertEquals(timeOut, timeoutConfiguration.getSocketTimeout());
		assertEquals(timeOut, timeoutConfiguration.getRequestTimeout());
	}
}
