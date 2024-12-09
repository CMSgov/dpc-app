package gov.cms.dpc.fhir.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@DisplayName("FHIR Client configuration")
class FHIRClientConfigurationUnitTest {
	private FHIRClientConfiguration fhirClientConfiguration = new FHIRClientConfiguration();
	@Test
        @DisplayName("Use setters and getters 🥳")
	public void testSettersAndGetters() {
		TimeoutConfiguration timeouts = new TimeoutConfiguration();

		fhirClientConfiguration.setTimeouts(timeouts);
		fhirClientConfiguration.setServerBaseUrl("url");
		assertEquals(timeouts, fhirClientConfiguration.getTimeouts());
		assertEquals("url", fhirClientConfiguration.getServerBaseUrl());
	}

}
