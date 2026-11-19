package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Practitioner endpoint tests
 */
class AttributionClientPractitionerUnitTest extends AbstractAttributionClientResourceUnitTest {

    @Test
    void testPractitionerSearchByNpi() {
        UUID organizationId = UUID.randomUUID();
        String providerNpi = "1234567890";
        IQuery<Bundle> query = mockSearch(Practitioner.class, Map.of(
                "organization", List.of(organizationId.toString()),
                "identifier", List.of(providerNpi)));
        Bundle bundle = new Bundle();
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchPractitioners(organizationId, providerNpi));
        verify(query).execute();
    }

    @Test
    void testPractitionerSearchById() {
        UUID organizationId = UUID.randomUUID();
        UUID practitionerId = UUID.randomUUID();
        IQuery<Bundle> query = mockSearch(Practitioner.class, Map.of(
                "organization", List.of(organizationId.toString()),
                "_id", List.of(practitionerId.toString())));
        Bundle bundle = new Bundle();
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchPractitioners(organizationId, practitionerId));
        verify(query).execute();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testPractitionerSearchWithoutNpi(String providerNpi) {
        UUID organizationId = UUID.randomUUID();
        IQuery<Bundle> query = mockSearch(Practitioner.class, Map.of("organization", List.of(organizationId.toString())));
        Bundle bundle = new Bundle();
        when(query.execute()).thenReturn(bundle);

        // Should return all practitioners
        assertSame(bundle, client.searchPractitioners(organizationId, providerNpi));
        verify(query).execute();
    }

    @Test
    void testGetPractitioner() {
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        practitioner.setId(practitionerId.toString());
        IReadExecutable<Practitioner> readExec = mockRead(Practitioner.class, practitionerId);
        when(readExec.execute()).thenReturn(practitioner);

        assertSame(practitioner, client.getPractitioner(practitionerId));
        verify(readExec).execute();
    }

    @Test
    void testGetPractitionerHandlesNotFound() {
        UUID practitionerId = UUID.randomUUID();
        IReadExecutable<Practitioner> readExec = mockRead(Practitioner.class, practitionerId);
        ResourceNotFoundException failure = new ResourceNotFoundException("Practitioner is not registered");
        when(readExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.getPractitioner(practitionerId)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testCreatePractitioner(boolean created) {
        Practitioner practitioner = new Practitioner();
        ICreateTyped createExec = mockCreate(practitioner);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(practitioner);
        outcome.setCreated(created);
        when(createExec.execute()).thenReturn(outcome);

        assertSame(outcome, client.createPractitioner(practitioner));
        verify(createExec).execute();
    }

    @Test
    void testCreatePractitionerHandlesTooManyProviders() {
        Practitioner practitioner = new Practitioner();
        ICreateTyped createExec = mockCreate(practitioner);
        UnprocessableEntityException failure = new UnprocessableEntityException("Provider limit reached");
        when(createExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(UnprocessableEntityException.class,
                () -> client.createPractitioner(practitioner)));
    }

    @Test
    void testSubmitPractitioners() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Practitioner());
        Bundle result = new Bundle();
        result.setType(Bundle.BundleType.COLLECTION);
        IOperationUntypedWithInput<Bundle> submitExec = mockSubmit(Practitioner.class, Bundle.class);
        when(submitExec.execute()).thenReturn(result);

        assertSame(result, client.submitPractitioners(bundle));
        ArgumentCaptor<Parameters> parameters = ArgumentCaptor.forClass(Parameters.class);
        verify(attributionClient.operation().onType(Practitioner.class).named("submit"))
                .withParameters(parameters.capture());
        assertEquals(1, parameters.getValue().getParameter().size());
        assertSame(bundle, parameters.getValue().getParameterFirstRep().getResource());
        verify(submitExec).execute();
    }

    @Test
    void testSubmitPractitionersHandlesException() {
        Bundle bundle = new Bundle();
        IOperationUntypedWithInput<Bundle> submitExec = mockSubmit(Practitioner.class, Bundle.class);
        UnprocessableEntityException failure = new UnprocessableEntityException("No practitioners submitted");
        when(submitExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(UnprocessableEntityException.class,
                () -> client.submitPractitioners(bundle)));
    }

    @Test
    void testUpdatePractitioner() {
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        Practitioner updatedPractitioner = new Practitioner();
        updatedPractitioner.setId(practitionerId.toString());
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(updatedPractitioner);
        IUpdateExecutable updateExec = mockUpdate(practitionerId, practitioner);
        when(updateExec.execute()).thenReturn(outcome);

        assertSame(updatedPractitioner, client.updatePractitioner(practitionerId, practitioner));
        verify(updateExec).execute();
    }

    @Test
    void testUpdatePractitionerNoResource() {
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        IUpdateExecutable updateExec = mockUpdate(practitionerId, practitioner);
        when(updateExec.execute()).thenReturn(new MethodOutcome());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> client.updatePractitioner(practitionerId, practitioner));
        assertEquals("Attribution service returned no Practitioner after update", failure.getMessage());
    }

    @Test
    void testDeletePractitioner() {
        UUID practitionerId = UUID.randomUUID();
        IDeleteTyped deleteExec = mockDelete(Practitioner.class, practitionerId);

        assertDoesNotThrow(() -> client.deletePractitioner(practitionerId));
        verify(deleteExec).execute();
    }

    @Test
    void testDeletePractitionerHandlesNotFound() {
        UUID practitionerId = UUID.randomUUID();
        IDeleteTyped deleteExec = mockDelete(Practitioner.class, practitionerId);
        ResourceNotFoundException failure = new ResourceNotFoundException("Practitioner is not registered");
        when(deleteExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.deletePractitioner(practitionerId)));
    }

}
