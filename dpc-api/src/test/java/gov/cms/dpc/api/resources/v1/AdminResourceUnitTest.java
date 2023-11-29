package gov.cms.dpc.api.resources.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;

public class AdminResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    AdminResource adminResource;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        adminResource = new AdminResource(attributionClient);
    }

    @Test
    public void testGetOrganizations() {
        UUID orgID1 = UUID.randomUUID();
        Organization organization1 = new Organization();
        organization1.setId(orgID1.toString());
        UUID orgID2 = UUID.randomUUID();
        Organization organization2 = new Organization();
        organization2.setId(orgID2.toString());
        String ids = orgID1.toString()+","+orgID2.toString();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization1);
        bundle.addEntry().setResource(organization2);
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("ids", Collections.singletonList(ids));

        @SuppressWarnings("unchecked")
        IQuery<IBaseBundle> queryExec = Mockito.mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
        @SuppressWarnings("unchecked")
        IQuery<Bundle> mockQuery = Mockito.mock(IQuery.class);
        Mockito.when(attributionClient.search().forResource(Organization.class)).thenReturn(queryExec);
        Mockito.when(queryExec.whereMap(searchParams)).thenReturn(queryExec);
        Mockito.when(queryExec.returnBundle(Bundle.class)).thenReturn(mockQuery);
        Mockito.when(queryExec.encodedJson()).thenReturn(queryExec);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        Bundle actualResponse = adminResource.getOrganizations(ids);
        assertEquals(2, actualResponse.getEntry().size());
    }
}
