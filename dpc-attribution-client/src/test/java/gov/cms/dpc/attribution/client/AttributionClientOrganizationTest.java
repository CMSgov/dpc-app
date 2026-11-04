package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests organization methods
 */
class AttributionClientOrganizationTest {

    private final IGenericClient attributionClient = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
    private final AttributionClient client = new AttributionClientImpl(attributionClient);

    @Test
    void testSearchOrganizationsByIds() {
        UUID orgID1 = UUID.randomUUID();
        Organization organization1 = new Organization();
        organization1.setId(orgID1.toString());
        UUID orgID2 = UUID.randomUUID();
        Organization organization2 = new Organization();
        organization2.setId(orgID2.toString());
        String ids = "id|" + orgID1 + "," + orgID2;
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization1);
        bundle.addEntry().setResource(organization2);
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("identifier", Collections.singletonList(ids));

        IQuery<Bundle> query = mockSearch(searchParams);
        when(query.execute()).thenReturn(bundle);

        Bundle actualResponse = client.searchOrganizations(ids);
        assertSame(bundle, actualResponse);
        assertEquals(2, actualResponse.getEntry().size());
        verify(query).execute();
    }

    @Test
    void testSearchOrganizationsByIdentifier() {
        String identifier = "1234567890";
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Organization());
        IQuery<Bundle> query = mockSearch(Map.of("identifier", List.of(identifier)));
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchOrganizations(identifier));
        verify(query).execute();
    }

    @Test
    void testSearchOrganizationsWithoutIdentifier() {
        Bundle bundle = new Bundle();
        IQuery<Bundle> query = mockSearch(Collections.emptyMap());
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchOrganizations(null));
        verify(query).execute();
    }

    @Test
    void testSubmitOrganization() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(organization);
        outcome.setCreated(true);

        @SuppressWarnings("unchecked")
        IOperationUntypedWithInput<MethodOutcome> submitExec = mock(IOperationUntypedWithInput.class);
        when(attributionClient
            .operation()
            .onType(Organization.class)
            .named("submit")
            .withParameters(any(Parameters.class))
            .returnMethodOutcome()
            .encodedJson()
        ).thenReturn(submitExec);
        when(submitExec.execute()).thenReturn(outcome);
        clearInvocations(attributionClient.operation().onType(Organization.class).named("submit"));

        assertSame(outcome, client.submitOrganization(bundle));
        ArgumentCaptor<Parameters> parameters = ArgumentCaptor.forClass(Parameters.class);
        verify(attributionClient.operation().onType(Organization.class).named("submit"))
            .withParameters(parameters.capture());
        assertEquals(1, parameters.getValue().getParameter().size());
        assertEquals("resource", parameters.getValue().getParameterFirstRep().getName());
        assertSame(bundle, parameters.getValue().getParameterFirstRep().getResource());
        verify(submitExec).execute();
    }

    @Test
    void testSubmitOrganizationHandlesFailure() {
        @SuppressWarnings("unchecked")
        IOperationUntypedWithInput<MethodOutcome> submitExec = mock(IOperationUntypedWithInput.class);
        when(attributionClient.operation().onType(Organization.class).named("submit")
                .withParameters(any(Parameters.class)).returnMethodOutcome().encodedJson()).thenReturn(submitExec);
        UnprocessableEntityException failure = new UnprocessableEntityException("Must provide organization to register");
        when(submitExec.execute()).thenThrow(failure);

        Bundle bundle = new Bundle();
        assertSame(failure, assertThrows(UnprocessableEntityException.class,
                () -> client.submitOrganization(bundle)));
    }

    @Test
    void testGetOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        IReadExecutable<Organization> readExec = mockRead(orgID);
        when(readExec.execute()).thenReturn(organization);

        assertSame(organization, client.getOrganization(orgID));
        verify(readExec).execute();
    }

    @Test
    void testGetOrganizationHandlesNotFound() {
        UUID orgID = UUID.randomUUID();
        IReadExecutable<Organization> readExec = mockRead(orgID);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find organization");
        when(readExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> client.getOrganization(orgID)));
    }

    @Test
    void testDeleteOrganization() {
        UUID orgID = UUID.randomUUID();
        IDeleteTyped deleteExec = mockDelete(orgID);

        assertDoesNotThrow(() -> client.deleteOrganization(orgID));
        verify(deleteExec).execute();
    }

    @Test
    void testDeleteOrganizationHandlesNotFound() {
        UUID orgID = UUID.randomUUID();
        IDeleteTyped deleteExec = mockDelete(orgID);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find organization");
        when(deleteExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> client.deleteOrganization(orgID)));
    }

    @Test
    void testUpdateOrganization() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(organization);
        IUpdateExecutable updateExec = mockUpdate(orgID, organization);
        when(updateExec.execute()).thenReturn(outcome);

        assertSame(organization, client.updateOrganization(orgID, organization));
        verify(updateExec).execute();
    }

    @Test
    void testUpdateOrganizationNoResource() {
        UUID orgID = UUID.randomUUID();
        Organization organization = new Organization();
        IUpdateExecutable updateExec = mockUpdate(orgID, organization);
        when(updateExec.execute()).thenReturn(new MethodOutcome());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> client.updateOrganization(orgID, organization));
        assertEquals("Attribution service returned no Organization after update", failure.getMessage());
    }

    @SuppressWarnings("unchecked")
    private IQuery<Bundle> mockSearch(Map<String, List<String>> searchParams) {
        IQuery<IBaseBundle> queryExec = mock(IQuery.class, RETURNS_DEEP_STUBS);
        IQuery<Bundle> mockQuery = mock(IQuery.class);
        when(attributionClient.search().forResource(Organization.class)).thenReturn(queryExec);
        when(queryExec.whereMap(searchParams)).thenReturn(queryExec);
        when(queryExec.encodedJson()).thenReturn(queryExec);
        when(queryExec.returnBundle(Bundle.class)).thenReturn(mockQuery);
        return mockQuery;
    }

    @SuppressWarnings("unchecked")
    private IReadExecutable<Organization> mockRead(UUID orgID) {
        IReadExecutable<Organization> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Organization.class).withId(orgID.toString())
                .encodedJson()).thenReturn(readExec);
        return readExec;
    }

    private IDeleteTyped mockDelete(UUID orgID) {
        IDeleteTyped deleteExec = mock(IDeleteTyped.class);
        when(attributionClient.delete().resourceById(new IdType("Organization", orgID.toString()))
                .encodedJson()).thenReturn(deleteExec);
        return deleteExec;
    }

    private IUpdateExecutable mockUpdate(UUID orgID, Organization organization) {
        IUpdateExecutable updateExec = mock(IUpdateExecutable.class);
        when(attributionClient.update().resource(organization).withId(orgID.toString())
                .encodedJson()).thenReturn(updateExec);
        return updateExec;
    }
}
