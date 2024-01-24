package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import io.reactivex.Flowable;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ResourceFetcherUnitTest {
    private static Patient testPatient;
    private static String testPatientMbi;
    private static String testPatientFhirId;

    @BeforeAll
    public static void setup() {
        MockBlueButtonClient bbClient = new MockBlueButtonClient(FhirContext.forDstu3());

        // Use an existing test patient from the MockBlueButtonClient
        testPatientMbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        Bundle bundle = bbClient.requestPatientFromServerByMbi(testPatientMbi, Map.of());
        testPatient = (Patient) bundle.getEntry().get(0).getResource();

        testPatientFhirId = testPatient.getIdElement().getIdPart();
    }

    @Test
    public void testHappyPath_Patient() {
        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> resources = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(1, resources.size());
        assertEquals(testPatient.getId(), resources.get(0).getId());
    }

    @Test
    public void testHappyPath_Eob() {
        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.ExplanationOfBenefit,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> resources = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(32, resources.size());
        assertTrue(resources.get(0).getId().contains("carrier-20587716665"));
    }

    @Test
    public void testHappyPath_coverage() {
        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Coverage,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> resources = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(3, resources.size());
        assertTrue(resources.get(0).getId().contains("part-a-20140000008325"));
    }

    @Test
    public void testHappyPath_NoSince() {
        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> resources = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(1, resources.size());
        assertEquals(testPatient.getId(), resources.get(0).getId());
    }

    @Test
    public void testBadTransactionTime() {
        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME.plus(1, ChronoUnit.DAYS)
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        JobQueueFailure exception = assertThrows(JobQueueFailure.class, () -> {
            results.flatMap(Flowable::fromIterable).toList().blockingGet();
        });

        assertEquals("BFD's transaction time regression", exception.getMessage());
    }

    @Test
    public void testResourceNotFound() {
        MockBlueButtonClient bbClient = Mockito.spy(new MockBlueButtonClient(FhirContext.forDstu3()));

        Mockito.doThrow(new ResourceNotFoundException("test exception"))
                .when(bbClient).requestPatientFromServer(eq(testPatientFhirId), any(), any());

        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                bbClient
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> errors = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(1, errors.size());
        OperationOutcome outcome = (OperationOutcome) errors.get(0);
        assertEquals(String.format("Patient resource not found in Blue Button for id: %s", testPatientMbi), outcome.getIssueFirstRep().getDetails().getText());
    }

    @Test
    public void testBaseServerErrorFetchingResources() {
        MockBlueButtonClient bbClient = Mockito.spy(new MockBlueButtonClient(FhirContext.forDstu3()));

        Mockito.doThrow(new FhirClientConnectionException("fhir client exception"))
                .when(bbClient).requestPatientFromServer(eq(testPatientFhirId), any(), any());

        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                bbClient
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> errors = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(1, errors.size());
        OperationOutcome outcome = (OperationOutcome) errors.get(0);
        assertEquals("Blue Button error fetching Patient resource. HTTP return code: 0", outcome.getIssueFirstRep().getDetails().getText());
    }

    @Test
    public void testUnknownErrorFetchingResources() {
        MockBlueButtonClient bbClient = Mockito.spy(new MockBlueButtonClient(FhirContext.forDstu3()));

        String exceptionMsg = "Unknown Exception";
        Mockito.doThrow(new IllegalStateException(exceptionMsg))
                .when(bbClient).requestPatientFromServer(eq(testPatientFhirId), any(), any());

        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                bbClient
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> errors = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(1, errors.size());
        OperationOutcome outcome = (OperationOutcome) errors.get(0);
        assertEquals(String.format("Internal error: %s", exceptionMsg), outcome.getIssueFirstRep().getDetails().getText());
    }

    @Test
    public void testWrongResourceTypeReturned() {
        MockBlueButtonClient bbClient = Mockito.spy(new MockBlueButtonClient(FhirContext.forDstu3()));

        // Build EoB bundle
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
        Bundle bundle = new Bundle();
        component.setResource(eob);
        bundle.addEntry(component);

        Mockito.doReturn(bundle).when(bbClient).requestPatientFromServer(eq(testPatientFhirId), any(), any());

        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Patient,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                bbClient
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());
        List<Resource> errors = results.flatMap(Flowable::fromIterable).toList().blockingGet();

        assertEquals(1, errors.size());
        OperationOutcome outcome = (OperationOutcome) errors.get(0);
        assertEquals("Internal error: Unexpected resource type: got ExplanationOfBenefit expected: Patient", outcome.getIssueFirstRep().getDetails().getText());
    }

    @Test
    public void testSearchForUnsupportedResourceType() {
        ResourceFetcher fetcher = getResourceFetcher(
                DPCResourceType.Practitioner,
                MockBlueButtonClient.TEST_LAST_UPDATED.minus(1, ChronoUnit.DAYS),
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        Flowable<List<Resource>> results = fetcher.fetchResources(testPatient, Map.of());

        JobQueueFailure exception = assertThrows(JobQueueFailure.class, () -> {
            results.flatMap(Flowable::fromIterable).toList().blockingGet();
        });

        assertTrue(exception.getMessage().contains("Unexpected resource type: Practitioner"));
    }

    private ResourceFetcher getResourceFetcher(DPCResourceType resourceType, OffsetDateTime since, OffsetDateTime transactionTime) {
        return getResourceFetcher(resourceType, since, transactionTime, new MockBlueButtonClient(FhirContext.forDstu3()));
    }

    private ResourceFetcher getResourceFetcher(DPCResourceType resourceType, OffsetDateTime since, OffsetDateTime transactionTime, MockBlueButtonClient bbClient) {
        return new ResourceFetcher(
                bbClient,
                UUID.randomUUID(),
                UUID.randomUUID(),
                resourceType,
                since,
                transactionTime
        );
    }
}