package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.dpc.api.r4.APITestHelpers;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.attribution.client.AttributionClient;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationResourceUnitTest {

    private AttributionClient attributionClient;
    private TokenDAO tokenDAO;
    private PublicKeyDAO publicKeyDAO;
    private OrganizationResource orgResource;

    @BeforeEach
    void setUp() {
        attributionClient = mock(AttributionClient.class);
        tokenDAO = mock(TokenDAO.class);
        publicKeyDAO = mock(PublicKeyDAO.class);
        orgResource = new OrganizationResource(attributionClient, tokenDAO, publicKeyDAO);
    }

    @Test
    void testSubmitOrganization() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(organization);
        when(attributionClient.submitOrganization(bundle)).thenReturn(outcome);

        assertSame(organization, orgResource.submitOrganization(bundle));
        verify(attributionClient).submitOrganization(bundle);
    }

    @Test
    void testSubmitOrganizationNoOrganization() {
        Bundle bundle = new Bundle();

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> orgResource.submitOrganization(bundle));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Bundle must include Organization", exception.getMessage());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testSubmitOrganizationNoResource() {
        Bundle bundle = new Bundle();
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        bundle.addEntry().setResource(organization);
        when(attributionClient.submitOrganization(bundle)).thenReturn(new MethodOutcome());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> orgResource.submitOrganization(bundle));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void testOrganizationSearch() {
        UUID orgID = UUID.fromString(APITestHelpers.ORGANIZATION_ID);
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        when(attributionClient.getOrganization(orgID)).thenReturn(organization);

        Bundle result = orgResource.orgSearch(APITestHelpers.makeOrganizationPrincipal());

        assertEquals(Bundle.BundleType.COLLECTION, result.getType());
        assertEquals(1, result.getTotal());
        assertSame(organization, result.getEntryFirstRep().getResource());
        verify(attributionClient).getOrganization(orgID);
        verifyNoMoreInteractions(attributionClient);
    }

    @Test
    void testGetOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        when(attributionClient.getOrganization(orgID)).thenReturn(organization);

        assertSame(organization, orgResource.getOrganization(orgID));
    }

    @Test
    void testDeleteOrganization() {
        UUID orgID = UUID.randomUUID();
        TokenEntity token = mock(TokenEntity.class);
        PublicKeyEntity key = mock(PublicKeyEntity.class);
        when(tokenDAO.fetchTokens(orgID)).thenReturn(List.of(token));
        when(publicKeyDAO.fetchPublicKeys(orgID)).thenReturn(List.of(key));

        Response response = orgResource.deleteOrganization(orgID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        InOrder order = inOrder(attributionClient, tokenDAO, publicKeyDAO);
        order.verify(attributionClient).deleteOrganization(orgID);
        order.verify(tokenDAO).deleteToken(token);
        order.verify(publicKeyDAO).deletePublicKey(key);
    }

    @Test
    void testUpdateOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        when(attributionClient.updateOrganization(orgID, organization)).thenReturn(organization);

        assertSame(organization, orgResource.updateOrganization(orgID, organization));
    }

    @Test
    void testUpdateOrganizationNoResource() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> orgResource.updateOrganization(orgID, organization));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Unable to update Organization", exception.getMessage());
    }
}
