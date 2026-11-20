package gov.cms.dpc.api.resources.r4;

import gov.cms.dpc.common.utils.PropertiesProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BaseResourceUnitTest {
    @Mock GroupResource gr;
    @Mock DataResource dr;
    @Mock OrganizationResource or;
    @Mock PractitionerResource pr;
    @Mock PatientResource par;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetterMethods() {
        BaseResource baseResource = new BaseResource(gr, dr, or, pr, par);

        assertEquals(gr, baseResource.groupOperations());
        assertEquals(dr, baseResource.dataOperations());
        assertEquals(or, baseResource.organizationOperations());
        assertEquals(pr, baseResource.practitionerOperations());
        assertEquals(par, baseResource.patientOperations());
    }

    @Test
    @Disabled("R4 metadata endpoint is not implemented")
    // TODO: Enable when metadata endpoint is implemented.
    void testMetadata() throws ReflectiveOperationException {
        BaseResource baseResource = new BaseResource(gr, dr, or, pr, par);

        assertInstanceOf(CapabilityStatement.class, BaseResource.class.getMethod("metadata").invoke(baseResource));
    }

    @Test
    @Disabled("R4 version endpoint is not implemented")
    // TODO: Enable when version endpoint is implemented.
    void testVersion() throws ReflectiveOperationException {
        try (MockedConstruction<PropertiesProvider> mock = Mockito.mockConstruction(PropertiesProvider.class)) {
            BaseResource baseResource = new BaseResource(gr, dr, or, pr, par);

            PropertiesProvider mockedPropertiesProvider = mock.constructed().get(0);
            Mockito.when(mockedPropertiesProvider.getBuildVersion()).thenReturn("version");

            assertEquals("version", BaseResource.class.getMethod("version").invoke(baseResource));
        }
    }
}
