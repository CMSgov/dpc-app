package gov.cms.dpc.fhir.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

/**
 * This test has no real practical value, but we need to get code coverage up to pass SonarQube checks on it.
 */
@DisplayName("Timeout configuration (code coverage only)")

class TimeoutConfigurationUnitTest {
	private TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration();
	@Test
	@DisplayName("Use setters and getters 🥳")
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
