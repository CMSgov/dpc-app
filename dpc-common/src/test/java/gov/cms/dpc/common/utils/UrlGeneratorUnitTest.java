package gov.cms.dpc.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlGeneratorUnitTest {

	@Test
	public void createUrl() {
		String result = UrlGenerator.generateVersionUrl(123);
		assertEquals("http://localhost:123/v1/version", result);
	}
}
