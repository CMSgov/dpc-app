package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.health.AggregationEngineHealthCheck;
import gov.cms.dpc.aggregation.service.ConsentResult;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.service.EveryoneGetsDataLookBackServiceImpl;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import org.assertj.core.util.Lists;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.commons.support.ReflectionSupport.invokeMethod;
import static org.mockito.Mockito.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
class AggregationEngineTest {
    private static final UUID aggregatorID = UUID.randomUUID();
    private static final String TEST_ORG_NPI = NPIUtil.generateNPI();
    private static final String TEST_PROVIDER_NPI = NPIUtil.generateNPI();
    private BlueButtonClient bbclient;
    private IJobQueue queue;
    private AggregationEngine engine;
    private Disposable subscribe;
    private ConsentService mockConsentService;

    private static final FhirContext fhirContext = FhirContext.forDstu3();
    private static final MetricRegistry metricRegistry = new MetricRegistry();
    private static final String EXPORT_PATH = "/tmp";

    @BeforeAll
    static void setupAll() {
        AggregationEngine.setGlobalErrorHandler();
        ContextUtils.prefetchResourceModels(fhirContext, JobQueueBatch.validResourceTypes);
    }

    @BeforeEach
    void setupEach() {
        mockConsentService = Mockito.mock(ConsentService.class);
        ConsentResult consentResult = new ConsentResult();
        consentResult.setConsentDate(new Date());
        consentResult.setActive(true);
        consentResult.setPolicyType(ConsentResult.PolicyType.OPT_IN);
        consentResult.setConsentId(UUID.randomUUID().toString());
        Mockito.when(mockConsentService.getConsent(List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)))).thenReturn(Optional.of(Lists.list(consentResult)));
        Mockito.when(mockConsentService.getConsent(List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(1)))).thenReturn(Optional.of(Lists.list(consentResult)));

        queue = Mockito.spy(new MemoryBatchQueue(10));
        bbclient = Mockito.spy(new MockBlueButtonClient(fhirContext));
        var operationalConfig = new OperationsConfig(1000, EXPORT_PATH, 500, YearMonth.of(2014, 3));
        LookBackService lookBackService = Mockito.spy(EveryoneGetsDataLookBackServiceImpl.class);
        JobBatchProcessor jobBatchProcessor = Mockito.spy(new JobBatchProcessor(bbclient, fhirContext, metricRegistry, operationalConfig, lookBackService, mockConsentService));
        engine = Mockito.spy(new AggregationEngine(aggregatorID, queue, operationalConfig, jobBatchProcessor));
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
        var patientMBI = MockBlueButtonClient.MBI_BENE_ID_MAP.get(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0));
        Bundle patient = bbclient.requestPatientFromServer(patientMBI, null, null);
        assertNotNull(patient);
    }

    /**
     * Verify that an exception in the claimBatch method doesn't kill polling the queue
     */
    @Test
    void claimBatchException() throws InterruptedException {
        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

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
        await().until(() -> !engine.isRunning());

        // The last mock doesn't get called because the engine gets stopped during the last call
        verify(queue, Mockito.times(10)).claimBatch(any(UUID.class));

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        assertEquals(1000, completeJob.getPriority());
        final var outputFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, completeJob.getBatchID(), DPCResourceType.Patient, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, completeJob.getBatchID(), DPCResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
        assertMDCReset();
    }

    /**
     * Verify that an exception in the processJobBatch method doesn't kill polling the queue
     */
    @Test
    void processJobBatchException() throws InterruptedException {
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
        await().until(() -> !engine.isRunning());

        verify(queue, Mockito.times(5)).claimBatch(any(UUID.class));
        assertMDCReset();
    }

    /**
     * Test if an engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void simpleJobTest() {
        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        assertEquals(1000, completeJob.getPriority());
        final var outputFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, completeJob.getBatchID(), DPCResourceType.Patient, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, completeJob.getBatchID(), DPCResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
        assertMDCReset();
    }

    /**
     * Test if an engine can handle a simple job with one resource type, one test provider, one patient and since.
     */
    @Test
    void sinceJobTest() {
        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(DPCResourceType.Patient),
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result. Should be not have any output file.
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        final var outputFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, completeJob.getBatchID(), DPCResourceType.Patient, 0);
        assertFalse(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, completeJob.getBatchID(), DPCResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
        assertMDCReset();
    }

    /**
     * Test if the engine can handle a job with multiple output files and patients
     */
    @Test
    void multipleFileJobTest() {
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                JobQueueBatch.validResourceTypes,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
        JobQueueBatch.validResourceTypes.forEach(resourceType -> {
            var outputFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, queue.getJobBatches(jobID).stream().findFirst().get().getBatchID(), resourceType, 0);
            assertTrue(Files.exists(Path.of(outputFilePath)));
        });
        assertMDCReset();
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
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"),
                JobQueueBatch.validResourceTypes,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

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
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                JobQueueBatch.validResourceTypes,
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

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
            var outputFilePath = ResourceWriter.formOutputFilePath(EXPORT_PATH, queue.getJobBatches(jobID).stream().findFirst().get().getBatchID(), resourceType, 0);
            assertTrue(Files.exists(Path.of(outputFilePath)));
        });
        assertMDCReset();
    }

    /**
     * Test that stopping the engine while a long running batch is still processing pauses the batch.
     */
    @Test
    void stopEngineDuringBatchProcessing() throws InterruptedException {
        // Create a batch that will hang forever
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_TIME_OUT);

        final var jobID = queue.createJob(
            orgID,
            TEST_ORG_NPI,
            TEST_PROVIDER_NPI,
            mbis,
            Collections.singletonList(DPCResourceType.Patient),
            null,
            MockBlueButtonClient.BFD_TRANSACTION_TIME,
            null, null, true, false);

        // Start an aggregation engine in a new thread and let it hang processing the batch
        Thread thread = new Thread(this.engine);
        thread.start();

        // Wait for the engine to pick up the thread, set status to RUNNING and hang on getting the patient from BFD
        JobQueueBatch batch = queue.getJobBatches(jobID).get(0);
        await().atMost(5, TimeUnit.SECONDS).until(() -> batch.getStatus() == JobStatus.RUNNING);

        // Stop the engine and make sure it pauses the batch
        assertEquals(JobStatus.RUNNING, batch.getStatus());
        engine.stop();
        assertEquals(JobStatus.QUEUED, batch.getStatus());
    }

    /**
     * Test if the engine can handle appending to a batch file with multiple patients
     */
    @Test
    void appendBatchFileTest() {
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // build a job with multiple resource types
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(
                () -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus())
        );
        var outputFilePath = Path.of(ResourceWriter.formOutputFilePath(EXPORT_PATH, queue.getJobBatches(jobID).stream().findFirst().get().getBatchID(), DPCResourceType.Patient, 0));
        assertTrue(Files.exists(outputFilePath));
        try {
            final String fileContents = Files.readString(outputFilePath);
            assertEquals(mbis.size(), Arrays.stream(fileContents.split("\n")).count(), "Contains multiple patients in file output");
        } catch (Exception e) {
            fail("Failed to read output file");
        }
        assertMDCReset();
    }

    /**
     * Test if the engine can handle a job with no attributions
     */
    @Test
    void emptyJobTest() {
        final var orgID = UUID.randomUUID();

        // Job with an unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                List.of(),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertFalse(queue.getJobBatches(jobID).stream().findFirst().isEmpty(), "Unable to retrieve job from queue.");
        queue.getJobBatches(jobID).stream().findFirst().ifPresent(retrievedJob -> {
            assertEquals(JobStatus.COMPLETED, retrievedJob.getStatus());
            assertEquals(0, retrievedJob.getJobQueueBatchFiles().size());
            assertFalse(Files.exists(Path.of(ResourceWriter.formOutputFilePath(EXPORT_PATH, retrievedJob.getBatchID(), DPCResourceType.Patient, 0))));
            assertFalse(Files.exists(Path.of(ResourceWriter.formOutputFilePath(EXPORT_PATH, retrievedJob.getBatchID(), DPCResourceType.OperationOutcome, 0))));
        });
        assertMDCReset();
    }


    /**
     * Test if the engine can handle a job with bad parameters
     */
    @Test
    void badJobTest() {
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // Job with an unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                Collections.singletonList(DPCResourceType.Schedule),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent(), "Unable to retrieve job from queue."),
                () -> assertEquals(JobStatus.FAILED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));
        assertMDCReset();
    }

    /**
     * Test if the engine can handle a job with bad parameters, and then fail marking the batch as failed
     */
    @Test
    void badJobTestWithFailBatchException() {
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1));

        // Job with an unsupported resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                Collections.singletonList(DPCResourceType.Schedule),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

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
        assertMDCReset();
    }

    /**
     * Test that the engine can handle a bad patient ID
     */
    @Test
    void badPatientIDTest() throws GeneralSecurityException {
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0), MockBlueButtonClient.TEST_PATIENT_MBIS.get(1), "-1");
        assertEquals(3, mbis.size());


        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                List.of(DPCResourceType.ExplanationOfBenefit, DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DateRangeParam> lastUpdatedCaptor = ArgumentCaptor.forClass(DateRangeParam.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServerByMbi(idCaptor.capture(), anyMap());
        Mockito.verify(bbclient, atLeastOnce()).requestEOBFromServer(idCaptor.capture(), lastUpdatedCaptor.capture(), anyMap());
        var values = idCaptor.getAllValues();
        assertEquals(1,
                values.stream().filter(value -> value.equals("-1")).count(),
                "Should be 1 call when loading the patient for consent check, then doesn't go any further");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(EXPORT_PATH, actual.getBatchID(), DPCResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(3, actual.getJobQueueBatchFiles().size(), "expected 3 (2 good patient ids and 1 bad patient id that failed lookback)"),
                () -> assertFalse(actual.getJobQueueFile(DPCResourceType.OperationOutcome).isEmpty(), "bad patient id fails lookback"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
        assertMDCReset();
    }

    @Test
    void multiplePatientsMatchTest() {
        final List<String> mbis = Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MULTIPLE_MBIS.get(0));

        final var orgID = UUID.randomUUID();

        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                List.of(DPCResourceType.ExplanationOfBenefit, DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(EXPORT_PATH, actual.getBatchID(), DPCResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(1, actual.getJobQueueBatchFiles().size(), "Should be one error file for lookback failure"),
                () -> assertFalse(actual.getJobQueueFile(DPCResourceType.OperationOutcome).isEmpty(), "Should be one error for lookback failure"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "Error file should not exist"));
        assertMDCReset();
    }

    @Test
    void testBlueButtonException() throws GeneralSecurityException {
        // Test generic runtime exception
        testWithThrowable(new RuntimeException("Error!!!!"));

        // Test with FhirSpecificError
        testWithThrowable(BaseServerResponseException.newInstance(500, "Sorry, can't do it"));

    }

    @Test
    void testUnhealthyIfProcessJobBatchThrowsException() throws InterruptedException {
        // This should never happen but if it does then this test is checking to make sure the look gets broken out
        // and goes into the #onError callback to set the queue to not running
        Mockito.doThrow(new RuntimeException("Error")).when(engine).processJobBatch(Mockito.any(JobQueueBatch.class));

        final var orgID = UUID.randomUUID();

        queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList("1"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null,  true, false);

        AggregationEngineHealthCheck healthCheck = new AggregationEngineHealthCheck(engine);
        assertTrue(healthCheck.check().isHealthy());

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(engine);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertFalse(healthCheck.check().isHealthy());
    }

    private void testWithThrowable(Throwable throwable) throws GeneralSecurityException {
        Mockito.reset(bbclient);
        ConsentResult consentResult = new ConsentResult();
        consentResult.setConsentDate(new Date());
        consentResult.setActive(true);
        consentResult.setPolicyType(ConsentResult.PolicyType.OPT_IN);
        consentResult.setConsentId(UUID.randomUUID().toString());
        Mockito.when(mockConsentService.getConsent("1")).thenReturn(Optional.of(Lists.list(consentResult)));
        // Override throwing an error on fetching a patient
        Mockito.doThrow(throwable).when(bbclient).requestPatientFromServer(Mockito.anyString(), Mockito.any(DateRangeParam.class), anyMap());

        final var orgID = UUID.randomUUID();

        // Make a simple job with one resource type
        final var jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList("1"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        // Work the batch
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        assertAll(() -> assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJobBatches(jobID).stream().findFirst().get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServerByMbi(idCaptor.capture(), anyMap());
        assertEquals(1, idCaptor.getAllValues().stream().filter(value -> value.equals("1")).count(), "Should have been called once to get the patient, but with errors instead");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJobBatches(jobID).stream().findFirst().isPresent());
        final var actual = queue.getJobBatches(jobID).stream().findFirst().get();
        var expectedErrorPath = ResourceWriter.formOutputFilePath(EXPORT_PATH, actual.getBatchID(), DPCResourceType.OperationOutcome, 0);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(1, actual.getJobQueueBatchFiles().size(), "expected just a operational outcome"),
                () -> assertFalse(actual.getJobQueueFile(DPCResourceType.OperationOutcome).isEmpty(), "expected 1 bad patient fetch"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }

    /**
     * Asserts that the MDC is empty except for the aggregator id.
     */
    private void assertMDCReset() {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        assertEquals(1, mdc.size());
        assertEquals(engine.getAggregatorID().toString(), mdc.get(MDCConstants.AGGREGATOR_ID));
    }

    @Nested
    class ErrorHandlerTest {

        static ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        static Method errorHandler;

        static final String ERR_MSG = "Caught exception during RxJava processing flow: ";
        static final String UNDELIVERABLE_EXC = "Undeliverable exception received: ";

        @BeforeAll
        static void setUp() throws NoSuchMethodException {
            Logger logger = (Logger) LoggerFactory.getLogger(AggregationEngine.class);
            logger.setLevel(Level.WARN);
            listAppender.start();
            logger.addAppender(listAppender);

            errorHandler = AggregationEngine.class.getDeclaredMethod("errorHandler", Throwable.class);
        }

        @BeforeEach
        void setUpEach() {
            listAppender.start();
        }

        @AfterEach
        void shutDownEach() {
            listAppender.stop();
            listAppender.list.clear();
        }

        @Test
        void undeliverableException() {
            invokeMethod(errorHandler, engine, new UndeliverableException(new Exception()));

            List<String> logMessages = getLogMessages();
            assertTrue(logMessages.contains(ERR_MSG));
            assertTrue(logMessages.contains(UNDELIVERABLE_EXC));
        }

        @Test
        void unknownException() {
            invokeMethod(errorHandler, engine, new Exception());

            List<String> logMessages = getLogMessages();
            assertTrue(logMessages.contains(ERR_MSG));
            assertTrue(logMessages.contains(UNDELIVERABLE_EXC));
        }

        @ParameterizedTest
        @MethodSource("provideExceptions")
        void otherExceptions(Exception e) {
            invokeMethod(errorHandler, engine, e);

            List<String> logMessages = getLogMessages();
            assertTrue(logMessages.contains(ERR_MSG));
            assertFalse(logMessages.contains(UNDELIVERABLE_EXC));
        }

        private static List<String> getLogMessages() {
            boolean success = false;
            int count = 0, maxTries = 50;
            List<String> logMessages = new ArrayList<>();

            // These tests throw non-stochastic ConcurrentModificationExceptions, this is a rough way to ignore them
            while (!success) {
                try {
                    logMessages = listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
                    success = true;
                } catch (ConcurrentModificationException e) {
                    if (count >= maxTries) throw e;
                    count++;
                }
            }

            return logMessages;
        }

        private static List<Exception> provideExceptions() {
            return List.of(
                    new IOException(),
                    new InterruptedException(),
                    new NullPointerException(),
                    new IllegalArgumentException(),
                    new IllegalStateException()
            );
        }
    }
}
