package gov.cms.dpc.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
@DisplayName("URL generators")


class UrlGeneratorUnitTest {
	@Test
	@DisplayName("Create URL 🥳")
public void createUrl() {
		String result = UrlGenerator.generateVersionUrl(123);
		assertEquals("http://localhost:123/v1/version", result);
	}

	@Test
	@DisplayName("Create URL with path 🥳")
public void createUrlWithPath() {
		String result = UrlGenerator.generateVersionUrl(123, "/path");
		assertEquals("http://localhost:123/path/v1/version", result);
	}
}
