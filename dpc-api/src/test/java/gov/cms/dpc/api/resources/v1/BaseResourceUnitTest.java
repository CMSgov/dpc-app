package gov.cms.dpc.api.resources.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.common.utils.PropertiesProvider;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.junit.jupiter.api.*;
import org.mockito.*;

class BaseResourceUnitTest {
    @Mock
    KeyResource kr;
    TokenResource tr;
    GroupResource gr;
    JobResource jr;
    DataResource dr;
    EndpointResource er;
    OrganizationResource or;
    PatientResource par;
    PractitionerResource pr;
    DefinitionResource sdr;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetterMethods() {
        BaseResource baseResource = new BaseResource(kr, tr, gr, jr, dr, er, or, par, pr, sdr, "baseURL");

        assertEquals(kr, baseResource.keyOperations());
        assertEquals(tr, baseResource.tokenOperations());
        assertEquals(gr, baseResource.groupOperations());
        assertEquals(jr, baseResource.jobOperations());
        assertEquals(sdr, baseResource.definitionResourceOperations());
        assertEquals(dr, baseResource.dataOperations());
        assertEquals(er, baseResource.endpointOperations());
        assertEquals(or, baseResource.organizationOperations());
        assertEquals(par, baseResource.patientOperations());
        assertEquals(pr, baseResource.practitionerOperations());
    }

    @Test
    public void testCapabilitiesStatement() {
        String baseURL = "baseURL";
        BaseResource baseResource = new BaseResource(kr, tr, gr, jr, dr, er, or, par, pr, sdr, baseURL);
        CapabilityStatement capStatement = new CapabilityStatement();

        try(MockedStatic<Capabilities> capabilities = Mockito.mockStatic(Capabilities.class) ) {
            capabilities.when(() -> Capabilities.getCapabilities(baseURL)).thenReturn(capStatement);
            assertEquals(capStatement, baseResource.metadata());
        }
    }

    @Test
    public void testVersion() {
        String version = "version";

        try(MockedConstruction<PropertiesProvider> ignored = mockConstruction(PropertiesProvider.class, (mock, context) ->
                when(mock.getBuildVersion()).thenReturn(version))) {
            BaseResource baseResource = new BaseResource(kr, tr, gr, jr, dr, er, or, par, pr, sdr, "baseURL");
            assertEquals(version, baseResource.version());
        }
    }
}
