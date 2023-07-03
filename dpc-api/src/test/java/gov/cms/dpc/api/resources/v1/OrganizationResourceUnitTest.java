package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
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
    public void testSubmitOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);

        @SuppressWarnings("unchecked")
        IOperationUntypedWithInput<Bundle> organizationBundle = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(any())
                .returnResourceType(Bundle.class)
                .encodedJson()
        ).thenReturn(organizationBundle);
        when(organizationBundle.execute()).thenReturn(bundle);

        Organization actualResponse = orgResource.submitOrganization(bundle);
        assertEquals(organization, actualResponse);
    }

    @Test
    public void testOrgSearch() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put(
                "organization",
                Collections.singletonList(organizationPrincipal.getOrganization().getIdElement().getIdPart())
        );
        Bundle bundle = new Bundle();

        @SuppressWarnings("unchecked")
        IQuery<IBaseBundle> queryExec = mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
        @SuppressWarnings("unchecked")
        IQuery<Bundle> mockQuery = mock(IQuery.class);

        when(attributionClient.search().forResource(Organization.class).encodedJson()).thenReturn(queryExec);
        when(queryExec.returnBundle(Bundle.class)).thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(bundle);
        when(mockQuery.whereMap(searchParams)).thenReturn(mockQuery);

        Bundle actualResponse = orgResource.orgSearch(organizationPrincipal);
        assertEquals(bundle, actualResponse);
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
        // TODO
    }
}
