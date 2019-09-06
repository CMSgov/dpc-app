package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.JobQueueInterface;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class AggregationEngineTest {
    private static final String TEST_PROVIDER_ID = "1";
    private BlueButtonClient bbclient;
    private JobQueueInterface queue;
    private AggregationEngineV2 engine;
    private Disposable subscribe;

    static private FhirContext fhirContext = FhirContext.forDstu3();
    static private MetricRegistry metricRegistry = new MetricRegistry();
    static private String exportPath;

    @BeforeAll
    static void setupAll() {
        final var config = ConfigFactory.load("dev-test.application.conf").getConfig("dpc.aggregation");
        exportPath = config.getString("exportPath");
        AggregationEngineV2.setGlobalErrorHandler();
        ContextUtils.prefetchResourceModels(fhirContext, JobQueueBatch.validResourceTypes);
    }

    @BeforeEach
    void setupEach() {
        queue = new MemoryBatchQueue(10);
        bbclient = Mockito.spy(new MockBlueButtonClient(fhirContext));
        var operationalConfig = new OperationsConfig(1000, exportPath);
        engine = new AggregationEngineV2(bbclient, queue, fhirContext, metricRegistry, operationalConfig);
        AggregationEngineV2.setGlobalErrorHandler();
        subscribe = Observable.empty().subscribe();
        engine.setSubscribe(subscribe);
    }

    /**
     * Test if the BB Mock Client will return a patient.
     */
    @Test
    void mockBlueButtonClientTest() {
        Patient patient = bbclient.requestPatientFromServer(MockBlueButtonClient.TEST_PATIENT_IDS.get(0));
        assertNotNull(patient);
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void simpleJobTest() {
        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_IDS.get(0)),
                Collections.singletonList(ResourceType.Patient)
        );

        // Work the batch
        queue.workBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.Patient, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if the engine can handle a job with multiple output files and patients
     */
    @Test
    void multipleFileJobTest() {
        final var orgID = UUID.randomUUID();

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_IDS.get(0)),
                JobQueueBatch.validResourceTypes
        );

        // Work the batch
        queue.workBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
        JobQueueBatch.validResourceTypes.forEach(resourceType -> {
            var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, queue.getJobBatches(jobID).stream().findFirst().get().getBatchID(), resourceType, 0);
            assertTrue(Files.exists(Path.of(outputFilePath)));
        });
    }

    /**
     * Test if the engine can handle a job with no attributions
     */
    @Test
    void emptyJobTest() {
        final var orgID = UUID.randomUUID();

        // Job with a unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                List.of(),
                Collections.singletonList(ResourceType.Patient)
        );

        // Work the batch
        queue.workBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertFalse(queue.getJobBatches(jobID).stream().findFirst().isEmpty(), "Unable to retrieve job from queue.");
        queue.getJobBatches(jobID).stream().findFirst().ifPresent(retrievedJob -> {
            assertEquals(JobStatus.COMPLETED, retrievedJob.getStatus());
            assertEquals(0, retrievedJob.getJobQueueBatchFiles().size());
            assertFalse(Files.exists(Path.of(ResourceWriter.formOutputFilePath(exportPath, retrievedJob.getBatchID(), ResourceType.Patient, 0))));
            assertFalse(Files.exists(Path.of(ResourceWriter.formOutputFilePath(exportPath, retrievedJob.getBatchID(), ResourceType.OperationOutcome, 0))));
        });
    }


    /**
     * Test if the engine can handle a job with bad parameters
     */
    @Test
    void badJobTest() {
        final var orgID = UUID.randomUUID();

        // Job with a unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_IDS,
                Collections.singletonList(ResourceType.Schedule)
        );

        // Work the batch
        queue.workBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent(), "Unable to retrieve job from queue."),
                () -> assertEquals(JobStatus.FAILED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
    }

    /**
     * Test that the engine can handle a bad patient ID
     */
    @Test
    void badPatientIDTest() {
        final List<String> patientIDs = new ArrayList<>(MockBlueButtonClient.TEST_PATIENT_IDS);
        // Add bad patient ID
        patientIDs.add("-1");
        assertEquals(3, patientIDs.size());

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                patientIDs,
                List.of(ResourceType.ExplanationOfBenefit, ResourceType.Patient)
        );

        // Work the batch
        queue.workBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServer(idCaptor.capture());
        Mockito.verify(bbclient, atLeastOnce()).requestEOBFromServer(idCaptor.capture());
        var values = idCaptor.getAllValues();
        assertEquals(6,
                values.stream().filter(value -> value.equals("-1")).count(),
                "Should be 6 invalid ids, 3 retries per method x 2 method calls x 1 bad-id");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(exportPath, actual.getBatchID(), ResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(4, actual.getJobQueueBatchFiles().size(), "expected 4 (= 2 output + 2 error)"),
                () -> assertEquals(1, actual.getJobQueueFile(ResourceType.OperationOutcome).orElseThrow().getCount(), "expected 1 for the one bad patient"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }

    @Test
    void testBlueButtonException() {
        // Test generic runtime exception
        testWithThrowable(new RuntimeException("Error!!!!"));

        // Test with FhirSpecificError
        testWithThrowable(BaseServerResponseException.newInstance(500, "Sorry, can't do it"));

    }

    private void testWithThrowable(Throwable throwable) {
        Mockito.reset(bbclient);
        // Override throwing an error on fetching a patient
        Mockito.doThrow(throwable).when(bbclient).requestPatientFromServer(Mockito.anyString());

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList("1"),
                Collections.singletonList(ResourceType.Patient)
        );

        // Work the batch
        queue.workBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServer(idCaptor.capture());
        assertEquals(3, idCaptor.getAllValues().stream().filter(value -> value.equals("1")).count(), "Should have been called 3 times to get the patient, but with errors instead");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(exportPath, actual.getBatchID(), ResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(1, actual.getJobQueueBatchFiles().size(), "expected just a operational outcome"),
                () -> assertEquals(1, actual.getJobQueueFile(ResourceType.OperationOutcome).orElseThrow().getCount(), "expected 1 bad patient fetch"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }
}