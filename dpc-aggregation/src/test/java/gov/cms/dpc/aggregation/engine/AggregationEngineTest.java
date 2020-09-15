package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.aggregation.health.AggregationEngineHealthCheck;
import gov.cms.dpc.aggregation.service.LookBackService;
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
import org.hl7.fhir.dstu3.model.Bundle;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private JobBatchProcessor jobBatchProcessor;
    private Disposable subscribe;
    private LookBackService lookBackService;

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
    void setupEach() throws ParseException {
        queue = Mockito.spy(new MemoryBatchQueue(10));
        bbclient = Mockito.spy(new MockBlueButtonClient(fhirContext));
        var operationalConfig = new OperationsConfig(1000, exportPath, 500, new SimpleDateFormat("dd/MM/yyyy").parse("03/01/2014"));
        lookBackService = Mockito.spy(LookBackService.class);
        jobBatchProcessor = Mockito.spy(new JobBatchProcessor(bbclient, fhirContext, metricRegistry, operationalConfig));
        engine = Mockito.spy(new AggregationEngine(aggregatorID, queue, operationalConfig, lookBackService, jobBatchProcessor));
        engine.queueRunning.set(true);
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
        Bundle patient = bbclient.requestPatientFromServer(MockBlueButtonClient.MBI_BENE_ID_MAP.get(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)), null);
        assertNotNull(patient);
    }

    /**
     * Verify that an exception in the claimBatch method doesn't kill polling the queue
     */
    @Test
    void claimBatchException() throws InterruptedException {
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        // Throw a failure on the first poll, then be successful
        JobQueueFailure ex = new JobQueueFailure("Any failure");

        doReturn(Optional.empty())
                .doThrow(ex)
                .doReturn(Optional.empty())
                .doReturn(Optional.empty())
                .doThrow(ex)
                .doReturn(Optional.empty())
                .doCallRealMethod()
                .doReturn(Optional.empty())
                .doReturn(Optional.empty())
                .doAnswer(invocationOnMock -> {
                    engine.stop();
                    return Optional.empty();
                })
                .when(queue)
                .claimBatch(any(UUID.class));

        engine.pollQueue();

        // Wait for the queue to finish processing before finishing the test
        while ( engine.isRunning() ) {
            Thread.sleep(100);
        }

        // The last mock doesn't get called because the engine gets stopped during the last call
        verify(queue, Mockito.times(10)).claimBatch(any(UUID.class));

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
     * Verify that an exception in the processJobBatch method doesn't kill polling the queue
     */
    @Test
    void processJobBatchException() throws InterruptedException {
        final var orgID = UUID.randomUUID();

        doReturn(Optional.empty())
                .doReturn(Optional.empty())
                .doReturn(Optional.empty())
                .doReturn(Optional.empty())
                .doAnswer(invocationOnMock -> {
                    engine.stop();
                    return Optional.empty();
                })
                .when(queue)
                .claimBatch(any(UUID.class));

        // Throw a failure on the third poll, then be successful
        JobQueueFailure ex = new JobQueueFailure("Any failure");
        doNothing()
                .doNothing()
                .doThrow(ex)
                .doNothing()
                .doNothing()
                .when(engine)
                .processJobBatch(any(JobQueueBatch.class));

        engine.pollQueue();

        // Wait for the queue to finish processing before finishing the test
        while ( engine.isRunning() ) {
            Thread.sleep(100);
        }

        verify(queue, Mockito.times(5)).claimBatch(any(UUID.class));
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void simpleJobTest() {
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
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
     * Test if a engine can handle a simple job with one resource type, one test provider, one patient and since.
     */
    @Test
    void sinceJobTest() {
        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.Patient),
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result. Should be not have any output file.
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.Patient, 0);
        assertFalse(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if the engine can handle a job with multiple output files and patients
     */
    @Test
    void multipleFileJobTest() {
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                JobQueueBatch.validResourceTypes,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
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
                JobQueueBatch.validResourceTypes,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
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
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                JobQueueBatch.validResourceTypes,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        // Work the batch
        engine.stop();
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Verify the queue is stopped
        assertFalse(engine.isRunning());
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
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
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
            assertEquals(mbis.size(), Arrays.stream(fileContents.split("\n")).count(), "Contains multiple patients in file output");
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
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
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
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // Job with a unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                Collections.singletonList(ResourceType.Schedule),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent(), "Unable to retrieve job from queue."),
                () -> assertEquals(JobStatus.FAILED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
    }

    /**
     * Test if the engine can handle a job with bad parameters, and then fail marking the batch as failed
     */
    @Test
    void badJobTestWithFailBatchException() {
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // Job with a unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                Collections.singletonList(ResourceType.Schedule),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        // Throw an exception when failing the batch
        Exception e = new RuntimeException("Failed to mark batch as failed");
        doThrow(e).when(queue).failBatch(any(JobQueueBatch.class), eq(aggregatorID));

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        // Job will be left in a running state, but that's okay, as the stuck batch logic will take over and retry the job in 5 minutes
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent(), "Unable to retrieve job from queue."),
                () -> assertEquals(JobStatus.RUNNING, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
    }

    /**
     * Test that the engine can handle a bad patient ID
     */
    @Test
    void badPatientIDTest() throws GeneralSecurityException {
        final var orgID = UUID.randomUUID();

        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(lookBackService).getPractitionerNPIFromRoster(orgID, TEST_PROVIDER_ID, "-1");
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1), "-1");
        assertEquals(3, mbis.size());


        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                List.of(ResourceType.ExplanationOfBenefit, ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DateRangeParam> lastUpdatedCaptor = ArgumentCaptor.forClass(DateRangeParam.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServerByMbi(idCaptor.capture());
        Mockito.verify(bbclient, atLeastOnce()).requestEOBFromServer(idCaptor.capture(), lastUpdatedCaptor.capture());
        var values = idCaptor.getAllValues();
        assertEquals(0,
                values.stream().filter(value -> value.equals("-1")).count(),
                "Should be 0 call, never makes it past lookback");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(exportPath, actual.getBatchID(), ResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(2, actual.getJobQueueBatchFiles().size(), "expected 2 (= 2 good patient ids and 1 bad patient id never made past lookback)"),
                () -> assertTrue(actual.getJobQueueFile(ResourceType.OperationOutcome).isEmpty(), "bad patient id never makes past lookback"),
                () -> assertFalse(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }

    @Test
    void multiplePatientsMatchTest() {
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        final List<String> mbis = Collections.singletonList(MockBlueButtonClient.MULTIPLE_RESULTS_MBI);

        final var orgID = UUID.randomUUID();

        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                mbis,
                List.of(ResourceType.ExplanationOfBenefit, ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(exportPath, actual.getBatchID(), ResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(0, actual.getJobQueueBatchFiles().size(), "Should be no files because it never made it past lookback"),
                () -> assertTrue(actual.getJobQueueFile(ResourceType.OperationOutcome).isEmpty(), "Should be no files because it never made it past lookback"),
                () -> assertFalse(Files.exists(Path.of(expectedErrorPath)), "Error file should not exist"));
    }

    @Test
    void testBlueButtonException() throws GeneralSecurityException {
        Mockito.doReturn(UUID.randomUUID().toString()).when(lookBackService).getPractitionerNPIFromRoster(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(lookBackService).hasClaimWithin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());

        // Test generic runtime exception
        testWithThrowable(new RuntimeException("Error!!!!"));

        // Test with FhirSpecificError
        testWithThrowable(BaseServerResponseException.newInstance(500, "Sorry, can't do it"));

    }

    @Test
    public void testUnhealthyIfProcessJobBatchThrowsException() throws InterruptedException {
        // This should never happen but if it does then this test is checking to make sure the look gets broken out
        // and goes into the #onError callback to set the queue to not running
        Mockito.doThrow(new RuntimeException("Error")).when(engine).processJobBatch(Mockito.any(JobQueueBatch.class));

        final var orgID = UUID.randomUUID();

        queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList("1"),
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
        );

        AggregationEngineHealthCheck healthCheck = new AggregationEngineHealthCheck(engine);
        Assert.assertTrue(healthCheck.check().isHealthy());

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(engine);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        Assert.assertFalse(healthCheck.check().isHealthy());
    }

    private void testWithThrowable(Throwable throwable) throws GeneralSecurityException {
        Mockito.reset(bbclient);
        // Override throwing an error on fetching a patient
        Mockito.doThrow(throwable).when(bbclient).requestPatientFromServer(Mockito.anyString(), Mockito.any(DateRangeParam.class));

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList("1"),
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME
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
        assertEquals(1, idCaptor.getAllValues().stream().filter(value -> value.equals("1")).count(), "Should have been called once to get the patient, but with errors instead");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(exportPath, actual.getBatchID(), ResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(0, actual.getJobQueueBatchFiles().size(), "expected just a operational outcome"),
                () -> assertTrue(actual.getJobQueueFile(ResourceType.OperationOutcome).isEmpty(), "expected 1 bad patient fetch"),
                () -> assertFalse(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }
}