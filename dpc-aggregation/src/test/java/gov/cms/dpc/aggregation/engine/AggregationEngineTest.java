package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.reactivex.disposables.Disposable;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
class AggregationEngineTest {
    private static final UUID aggregatorID = UUID.randomUUID();
    private static final String TEST_PROVIDER_ID = "1";
    private BlueButtonClient bbclient;
    private IJobQueue queue;
    private AggregationEngine engine;
    private Disposable subscribe;

    static private FhirContext fhirContext = FhirContext.forDstu3();
    static private MetricRegistry metricRegistry = new MetricRegistry();
    static private String exportPath;

    @BeforeAll
    static void setupAll() {
        final var config = ConfigFactory.load("testing.conf").getConfig("dpc.aggregation");
        exportPath = config.getString("exportPath");
        AggregationEngine.setGlobalErrorHandler();
        ContextUtils.prefetchResourceModels(fhirContext, JobQueueBatch.validResourceTypes);
    }

    @BeforeEach
    void setupEach() {
        queue = Mockito.spy(new MemoryBatchQueue(10));
        bbclient = Mockito.spy(new MockBlueButtonClient(fhirContext));
        var operationalConfig = new OperationsConfig(1000, exportPath, 500);
        engine = new AggregationEngine(aggregatorID, bbclient, queue, fhirContext, metricRegistry, operationalConfig);
        AggregationEngine.setGlobalErrorHandler();
        subscribe = Mockito.mock(Disposable.class);
        doReturn(false).when(subscribe).isDisposed();
        engine.setSubscribe(subscribe);
    }

    /**
     * Test if the BB Mock Client will return a patient.
     */
    @Test
    void mockBlueButtonClientTest() {
        Patient patient = bbclient.requestPatientFromServer(MockBlueButtonClient.MBI_BENE_ID_MAP.get(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)));
        assertNotNull(patient);
    }

    /**
     * Verify that an exception in the claimBatch method doesn't kill polling the queue
     */
    @Test
    void claimBatchException() throws InterruptedException {
        // Throw a failure on the first poll, then be successful
        JobQueueFailure ex = new JobQueueFailure("Any failure");
        when(queue.claimBatch(any(UUID.class)))
                .thenThrow(ex)
                .thenAnswer(invocationOnMock -> {
                    engine.stop();
                    return Optional.empty();
                });

        engine.pollQueue();

        // Wait for the queue to finish processing before finishing the test
        while ( engine.isRunning() ) {
            Thread.sleep(100);
        }

        verify(queue, Mockito.times(2)).claimBatch(any(UUID.class));
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
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.Patient)
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        assertEquals(1000, completeJob.getPriority());
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
                new ArrayList<>(MockBlueButtonClient.MBI_BENE_ID_MAP.keySet()),
                JobQueueBatch.validResourceTypes
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
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
     * Test if the engine can split a job into multiple batches
     */
    @Test
    void multipleBatchJobTest() {
        final var orgID = UUID.randomUUID();

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"),
                JobQueueBatch.validResourceTypes
        );

        // Assert the queue size
        assertEquals(2, queue.queueSize());
        assertEquals(5000, queue.getJobBatches(jobID).get(0).getPriority());
    }

    /**
     * Test if the engine can handle a pausing a job on shutdown
     */
    @Test
    void pauseJobTest() {
        final var orgID = UUID.randomUUID();

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                new ArrayList<>(MockBlueButtonClient.MBI_BENE_ID_MAP.keySet()),
                JobQueueBatch.validResourceTypes
        );

        // Work the batch
        engine.stop();
        doReturn(true).when(subscribe).isDisposed();
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Verify disposed call
        Mockito.verify(subscribe).dispose();

        // Look at the result
        assertAll(
                () -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.QUEUED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()),
                () -> assertEquals(0, queue.getJobBatches(jobID).stream().findFirst().get().getPatientIndex().get(), "Has processed one patient before pausing")
        );
        JobQueueBatch.validResourceTypes.forEach(resourceType -> {
            var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, queue.getJobBatches(jobID).stream().findFirst().get().getBatchID(), resourceType, 0);
            assertTrue(Files.exists(Path.of(outputFilePath)));
        });
    }

    /**
     * Test if the engine can handle appending to a batch file with multiple patients
     */
    @Test
    void appendBatchFileTest() {
        final var orgID = UUID.randomUUID();

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                new ArrayList<>(MockBlueButtonClient.MBI_BENE_ID_MAP.keySet()),
                Collections.singletonList(ResourceType.Patient)
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(
                () -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus())
        );
        var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, queue.getJobBatches(jobID).stream().findFirst().get().getBatchID(), ResourceType.Patient, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        try {
            final String fileContents = Files.readString(Path.of(outputFilePath));
            assertEquals(MockBlueButtonClient.TEST_PATIENT_MBIS.size(), Arrays.stream(fileContents.split("\n")).count(), "Contains multiple patients in file output");
        } catch ( Exception e ) {
            Assert.fail("Failed to read output file");
        }
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
        queue.claimBatch(engine.getAggregatorID())
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
                new ArrayList<>(MockBlueButtonClient.MBI_BENE_ID_MAP.keySet()),
                Collections.singletonList(ResourceType.Schedule)
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent(), "Unable to retrieve job from queue."),
                () -> assertEquals(JobStatus.FAILED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
    }

    /**
     * Test that the engine can handle a bad patient ID
     */
    @Test
    void badPatientIDTest() throws GeneralSecurityException {
        final List<String> mbis = new ArrayList<>(MockBlueButtonClient.MBI_BENE_ID_MAP.keySet());
        // Add bad patient ID
        mbis.add("-1");
        assertEquals(3, mbis.size());

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                List.of(ResourceType.ExplanationOfBenefit, ResourceType.Patient)
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServerByMbi(idCaptor.capture());
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
                () -> assertEquals(3, actual.getJobQueueBatchFiles().size(), "expected 3 (= 2 output + 1 error)"),
                () -> assertEquals(2, actual.getJobQueueFile(ResourceType.OperationOutcome).orElseThrow().getCount(), "expected 2 for the one bad patient (eob + patient)"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }

    @Test
    void testBlueButtonException() throws GeneralSecurityException {
        // Test generic runtime exception
        testWithThrowable(new RuntimeException("Error!!!!"));

        // Test with FhirSpecificError
        testWithThrowable(BaseServerResponseException.newInstance(500, "Sorry, can't do it"));

    }

    private void testWithThrowable(Throwable throwable) throws GeneralSecurityException {
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
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServerByMbi(idCaptor.capture());
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