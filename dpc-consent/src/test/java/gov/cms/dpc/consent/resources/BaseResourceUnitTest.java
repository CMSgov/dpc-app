package gov.cms.dpc.consent.resources;

import gov.cms.dpc.common.utils.PropertiesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseResourceUnitTest {

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

    @Test
	public void testGetVersion() {
        try(MockedConstruction<PropertiesProvider> mock = Mockito.mockConstruction(PropertiesProvider.class)) {
            BaseResource baseResource = new BaseResource();

            PropertiesProvider mockedPropertiesProvider = mock.constructed().get(0);
            Mockito.when(mockedPropertiesProvider.getBuildVersion()).thenReturn("version");

            assertEquals("version", baseResource.version());
        }
	}
}
