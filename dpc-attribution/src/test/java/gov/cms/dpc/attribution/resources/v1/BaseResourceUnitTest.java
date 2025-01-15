package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.common.utils.PropertiesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Disabled // TODO: still valid? mocking not working as expected -acw
	@Test
	public void testGetVersion() {
		doReturn("version").when(pp).getBuildVersion();
		assertEquals("version", baseResource.version());
	}
}
