package gov.cms.dpc.consent.resources;

import gov.cms.dpc.common.utils.PropertiesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.doReturn;

class BaseResourceUnitTest {
	@InjectMocks
	BaseResource baseResource;

	@Mock
	PropertiesProvider pp;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
        @DisplayName("Get base resource version 🥳")
	public void testGetVersion() {
		doReturn("version").when(pp).getBuildVersion();
		assertEquals("version", baseResource.version());
	}
}
