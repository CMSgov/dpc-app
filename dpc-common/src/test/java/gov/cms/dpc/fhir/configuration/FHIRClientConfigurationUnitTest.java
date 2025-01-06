package gov.cms.dpc.fhir.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FHIRClientConfigurationUnitTest {
	private final FHIRClientConfiguration fhirClientConfiguration = new FHIRClientConfiguration();
	@Test
	public void testSettersAndGetters() {
		TimeoutConfiguration timeouts = new TimeoutConfiguration();

		fhirClientConfiguration.setTimeouts(timeouts);
		fhirClientConfiguration.setServerBaseUrl("url");
		assertEquals(timeouts, fhirClientConfiguration.getTimeouts());
		assertEquals("url", fhirClientConfiguration.getServerBaseUrl());
	}

}
