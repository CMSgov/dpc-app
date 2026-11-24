package gov.cms.dpc.api.resources.r4;

import gov.cms.dpc.attribution.client.AttributionClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.MockitoAnnotations.openMocks;

public class AdminResourceUnitTest {

    @Mock
    AttributionClient attributionClient;
    AdminResource adminResource;

    @BeforeEach
    void setUp() {
        openMocks(this);
        adminResource = new AdminResource(attributionClient);
    }

    @Test
    void testGetOrganizations() {
        UUID orgID1 = UUID.randomUUID();
        Organization organization1 = new Organization();
        organization1.setId(orgID1.toString());
        UUID orgID2 = UUID.randomUUID();
        Organization organization2 = new Organization();
        organization2.setId(orgID2.toString());
        String ids = "id|"+ orgID1 +","+ orgID2;
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization1);
        bundle.addEntry().setResource(organization2);
        Mockito.when(attributionClient.searchOrganizations(ids)).thenReturn(bundle);

        Bundle actualResponse = adminResource.getOrganizations(ids);
        assertEquals(2, actualResponse.getEntry().size());
    }

    @Test
    void testGetOrganizations_noResults() {
        String ids = "id|" + UUID.randomUUID();
        Bundle emptyBundle = new Bundle();
        Mockito.when(attributionClient.searchOrganizations(ids)).thenReturn(emptyBundle);

        Bundle actualResponse = adminResource.getOrganizations(ids);

        assertEquals(0, actualResponse.getEntry().size());
    }

    @Test
    void testGetOrganizations_singleResult() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        String ids = "id|" + orgID;
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);
        Mockito.when(attributionClient.searchOrganizations(ids)).thenReturn(bundle);

        Bundle actualResponse = adminResource.getOrganizations(ids);

        assertEquals(1, actualResponse.getEntry().size());
        assertEquals(orgID.toString(), actualResponse.getEntryFirstRep().getResource().getIdElement().getIdPart());
    }

    @Test
    void testGetOrganizations_passesNpisThroughUnchanged() {
        String ids = "id|" + UUID.randomUUID() + "," + UUID.randomUUID();
        Mockito.when(attributionClient.searchOrganizations(ids)).thenReturn(new Bundle());

        adminResource.getOrganizations(ids);

        Mockito.verify(attributionClient).searchOrganizations(ids);
    }

    @Test
    void testGetOrganizations_propagatesClientException() {
        String ids = "id|" + UUID.randomUUID();
        Mockito.when(attributionClient.searchOrganizations(ids))
                .thenThrow(new RuntimeException("Attribution service unavailable"));

        assertThrows(RuntimeException.class, () -> adminResource.getOrganizations(ids));
    }
}
