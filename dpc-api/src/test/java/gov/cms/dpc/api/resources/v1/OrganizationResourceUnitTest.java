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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganizationResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;
    OrganizationResource orgResource;

    TokenDAO tokenDAO = Mockito.mock(TokenDAO.class);
    PublicKeyDAO publicKeyDAO = Mockito.mock(PublicKeyDAO.class);

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        orgResource = new OrganizationResource(attributionClient, tokenDAO, publicKeyDAO);
    }

//    @Test
//    public void testSubmitOrganization() {
//        UUID orgID = UUID.randomUUID();
//        Organization organization = new Organization();
//        organization.setId(orgID.toString());
//        Bundle bundle = new Bundle();
//        bundle.addEntry().setResource(organization);
//
//        @SuppressWarnings("unchecked")
//        IOperationUntypedWithInput<Bundle> organizationBundle = Mockito.mock(IOperationUntypedWithInput.class);
//        Mockito.when(attributionClient
//                .operation()
//                .onType(Organization.class)
//                .named("submit")
//                .withParameters(Mockito.any())
//                .returnResourceType(Bundle.class)
//                .encodedJson()
//        ).thenReturn(organizationBundle);
//        Mockito.when(organizationBundle.execute()).thenReturn(bundle);
//
//        Organization actualResponse = orgResource.submitOrganization(bundle);
//        assertEquals(organization, actualResponse);
//    }

//    @Test
//    public void testOrgSearch() {
//        UUID orgID = UUID.randomUUID();
//        Organization organization = new Organization();
//        organization.setId(orgID.toString());
//        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
//        Map<String, List<String>> searchParams = new HashMap<>();
//        searchParams.put(
//                "organization",
//                Collections.singletonList(organizationPrincipal.getOrganization().getIdElement().getIdPart())
//        );
//        Bundle bundle = new Bundle();
//
//        @SuppressWarnings("unchecked")
//        IQuery<IBaseBundle> queryExec = Mockito.mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
//        @SuppressWarnings("unchecked")
//        IQuery<Bundle> mockQuery = Mockito.mock(IQuery.class);
//
//        Mockito.when(attributionClient.search().forResource(Organization.class).encodedJson()).thenReturn(queryExec);
//        Mockito.when(queryExec.returnBundle(Bundle.class)).thenReturn(mockQuery);
//        Mockito.when(mockQuery.execute()).thenReturn(bundle);
//        Mockito.when(mockQuery.whereMap(searchParams)).thenReturn(mockQuery);
//
//        Bundle actualResponse = orgResource.orgSearch(organizationPrincipal);
//        assertEquals(bundle, actualResponse);
//    }

    @Test
    public void testGetOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());

        @SuppressWarnings("unchecked")
        IReadExecutable<Organization> readExec = Mockito.mock(IReadExecutable.class);

        Mockito.when(attributionClient.read().resource(Organization.class).withId(orgID.toString()).encodedJson()).thenReturn(readExec);
        Mockito.when(readExec.execute()).thenReturn(organization);

        Organization actualResponse = orgResource.getOrganization(orgID);

        assertEquals(organization, actualResponse);
    }

    @Test
    public void testDeleteOrganization() {
        UUID orgID = UUID.randomUUID();

        IDeleteTyped delRet = Mockito.mock(IDeleteTyped.class);
        Mockito.when(attributionClient.delete().resourceById(new IdType("Organization", orgID.toString())).encodedJson()).thenReturn(delRet);

        Response actualResponse = orgResource.deleteOrganization(orgID);

        assertEquals(200, actualResponse.getStatus());
    }

    @Test
    public void testUpdateOrganization() {
        // TODO
    }
}
