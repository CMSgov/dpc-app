package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class OrganizationResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;
    OrganizationResource orgResource;

    TokenDAO tokenDAO = mock(TokenDAO.class);
    PublicKeyDAO publicKeyDAO = mock(PublicKeyDAO.class);

    @BeforeEach
    public void setUp() {
        openMocks(this);
        orgResource = new OrganizationResource(attributionClient, tokenDAO, publicKeyDAO);
    }

    @Test
    public void testSubmitOrganizationNoEndpoints() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);

        assertThrows(WebApplicationException.class, () -> orgResource.submitOrganization(bundle));
    }

    @Test
    public void testGetOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());

        @SuppressWarnings("unchecked")
        IReadExecutable<Organization> readExec = mock(IReadExecutable.class);

        when(attributionClient
                .read()
                .resource(Organization.class)
                .withId(orgID.toString())
                .encodedJson()
        ).thenReturn(readExec);
        when(readExec.execute()).thenReturn(organization);

        Organization actualResponse = orgResource.getOrganization(orgID);
        assertEquals(organization, actualResponse);
    }

    @Test
    public void testDeleteOrganization() {
        UUID orgID = UUID.randomUUID();

        IDeleteTyped delRet = mock(IDeleteTyped.class);
        when(attributionClient
                .delete()
                .resourceById(new IdType("Organization", orgID.toString()))
                .encodedJson()
        ).thenReturn(delRet);

        Response actualResponse = orgResource.deleteOrganization(orgID);
        assertEquals(200, actualResponse.getStatus());
    }

    @Test
    public void testUpdateOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());

        IUpdateExecutable updateExec = mock(IUpdateExecutable.class);
        MethodOutcome outcome = mock(MethodOutcome.class);
        when(outcome.getResource()).thenReturn(organization);
        when(attributionClient
                .update()
                .resource(organization)
                .withId(orgID.toString())
                .encodedJson()
        ).thenReturn(updateExec);
        when(updateExec.execute()).thenReturn(outcome);

        Organization actualResponse = orgResource.updateOrganization(orgID, organization);
        assertEquals(organization, actualResponse);
    }
}
