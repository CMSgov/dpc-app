package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.dpc.attribution.client.service.ConsentService;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Shared HAPI client mocks for attribution resource unit tests.
 */
abstract class AbstractAttributionClientResourceUnitTest {

    protected final IGenericClient attributionClient = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
    protected final ConsentService consentService = mock(ConsentService.class);
    protected final AttributionClient client = new AttributionClient(attributionClient, consentService);

    @SuppressWarnings("unchecked")
    protected IQuery<Bundle> mockSearch(Class<? extends Resource> resourceClass,
                                      Map<String, List<String>> searchParams) {
        IQuery<Bundle> mockQuery = mock(IQuery.class);
        stubSearch(resourceClass, mockQuery);
        when(mockQuery.whereMap(searchParams)).thenReturn(mockQuery);
        return mockQuery;
    }

    @SuppressWarnings("unchecked")
    protected IQuery<Bundle> mockSearch(Class<? extends Resource> resourceClass) {
        IQuery<Bundle> mockQuery = mock(IQuery.class, RETURNS_SELF);
        stubSearch(resourceClass, mockQuery);
        return mockQuery;
    }

    @SuppressWarnings("unchecked")
    private void stubSearch(Class<? extends Resource> resourceClass, IQuery<Bundle> mockQuery) {
        IQuery<IBaseBundle> queryExec = mock(IQuery.class, RETURNS_SELF);
        when(attributionClient.search().forResource(resourceClass)).thenReturn(queryExec);
        when(queryExec.returnBundle(Bundle.class)).thenReturn(mockQuery);
    }

    protected IQuery<Bundle> mockSearch(Class<? extends Resource> resourceClass,
                                      Map<String, List<String>> searchParams, String tagSystem, String tag) {
        IQuery<Bundle> mockQuery = mockSearch(resourceClass, searchParams);
        when(mockQuery.withTag(tagSystem, tag)).thenReturn(mockQuery);
        return mockQuery;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Resource> IOperationUntypedWithInput<T> mockInstanceOperation(
            Class<? extends Resource> resourceClass, UUID id, String operationName, Class<T> responseClass) {
        IOperationUntypedWithInput<T> operationExec = mock(IOperationUntypedWithInput.class, RETURNS_SELF);
        when(attributionClient.operation().onInstance(new IdType(resourceClass.getSimpleName(), id.toString()))
                .named(operationName).withParameters(any(Parameters.class)).returnResourceType(responseClass))
                .thenReturn(operationExec);
        clearInvocations(attributionClient.operation()
                .onInstance(new IdType(resourceClass.getSimpleName(), id.toString())).named(operationName));
        return operationExec;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Resource> IReadExecutable<T> mockRead(Class<T> resourceClass, UUID id) {
        IReadExecutable<T> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(resourceClass).withId(id.toString())
                .encodedJson()).thenReturn(readExec);
        return readExec;
    }

    protected ICreateTyped mockCreate(Resource resource) {
        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(resource).encodedJson()).thenReturn(createExec);
        return createExec;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Resource> IOperationUntypedWithInput<T> mockSubmit(
            Class<? extends Resource> resourceClass, Class<T> responseClass) {
        IOperationUntypedWithInput<T> submitExec = mock(IOperationUntypedWithInput.class);
        when(attributionClient.operation().onType(resourceClass).named("submit")
                .withParameters(any(Parameters.class)).returnResourceType(responseClass)
                .encodedJson()).thenReturn(submitExec);
        clearInvocations(attributionClient.operation().onType(resourceClass).named("submit"));
        return submitExec;
    }

    @SuppressWarnings("unchecked")
    protected IOperationUntypedWithInput<MethodOutcome> mockSubmitMethodOutcome(
            Class<? extends Resource> resourceClass) {
        IOperationUntypedWithInput<MethodOutcome> submitExec = mock(IOperationUntypedWithInput.class);
        when(attributionClient.operation().onType(resourceClass).named("submit")
                .withParameters(any(Parameters.class)).returnMethodOutcome()
                .encodedJson()).thenReturn(submitExec);
        clearInvocations(attributionClient.operation().onType(resourceClass).named("submit"));
        return submitExec;
    }

    protected IUpdateExecutable mockUpdate(UUID id, Resource resource) {
        IUpdateExecutable updateExec = mock(IUpdateExecutable.class);
        when(attributionClient.update().resource(resource).withId(id.toString())
                .encodedJson()).thenReturn(updateExec);
        return updateExec;
    }

    protected IDeleteTyped mockDelete(Class<? extends Resource> resourceClass, UUID id) {
        IDeleteTyped deleteExec = mock(IDeleteTyped.class);
        when(attributionClient.delete().resourceById(new IdType(resourceClass.getSimpleName(), id.toString()))
                .encodedJson()).thenReturn(deleteExec);
        return deleteExec;
    }
}
