package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.common.utils.PropertiesProvider;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseResourceUnitTest {
    @Mock KeyResource kr;
    @Mock TokenResource tr;
    @Mock GroupResource gr;
    @Mock JobResource jr;
    @Mock DataResource dr;
    @Mock OrganizationResource or;
    @Mock PatientResource par;
    @Mock PractitionerResource pr;
    @Mock DefinitionResource sdr;
    @Mock AdminResource ar;
    @Mock IpAddressResource ip;

    String url = "baseURL";


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetterMethods() {
        BaseResource baseResource = new BaseResource(kr, tr, gr, jr, dr, or, par, pr, sdr, ar, ip, url);

        assertEquals(kr, baseResource.keyOperations());
        assertEquals(tr, baseResource.tokenOperations());
        assertEquals(gr, baseResource.groupOperations());
        assertEquals(jr, baseResource.jobOperations());
        assertEquals(sdr, baseResource.definitionResourceOperations());
        assertEquals(dr, baseResource.dataOperations());
        assertEquals(or, baseResource.organizationOperations());
        assertEquals(par, baseResource.patientOperations());
        assertEquals(pr, baseResource.practitionerOperations());
        assertEquals(ar, baseResource.adminOperations());
        assertEquals(ip, baseResource.ipAddressOperations());
    }

    @Test
    void testMetadata() {
        BaseResource baseResource = new BaseResource(kr, tr, gr, jr, dr, or, par, pr, sdr, ar, ip, url);
        CapabilityStatement capabilityStatement = Mockito.mock(CapabilityStatement.class);

        try(MockedStatic<Capabilities> capabilities = Mockito.mockStatic(Capabilities.class)) {
            capabilities.when(() -> Capabilities.getCapabilities(url))
                    .thenReturn(capabilityStatement);
            assertEquals(capabilityStatement, baseResource.metadata());
        }
    }

    @Test
    void testVersion() {
        try(MockedConstruction<PropertiesProvider> mock = Mockito.mockConstruction(PropertiesProvider.class)) {
            BaseResource baseResource = new BaseResource(kr, tr, gr, jr, dr, or, par, pr, sdr, ar, ip, url);

            PropertiesProvider mockedPropertiesProvider = mock.constructed().get(0);
            Mockito.when(mockedPropertiesProvider.getBuildVersion()).thenReturn("version");

            assertEquals("version", baseResource.version());
        }
    }
}
