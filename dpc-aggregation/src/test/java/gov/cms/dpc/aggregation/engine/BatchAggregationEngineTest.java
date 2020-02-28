package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.reactivex.disposables.Disposable;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(BufferedLoggerHandler.class)
class BatchAggregationEngineTest {
    private static final UUID aggregatorID = UUID.randomUUID();
    private static final String TEST_PROVIDER_ID = "1";
    private IJobQueue queue;
    private AggregationEngine engine;
    private Disposable subscribe;

    static private FhirContext fhirContext = FhirContext.forDstu3();
    static private MetricRegistry metricRegistry = new MetricRegistry();
    static private String exportPath;
    static private OperationsConfig operationsConfig;

    @BeforeAll
    static void setupAll() {
        fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
        final var config = ConfigFactory.load("testing.conf").getConfig("dpc.aggregation");
        exportPath = config.getString("exportPath");
        operationsConfig = new OperationsConfig(10, exportPath, 3);
        AggregationEngine.setGlobalErrorHandler();
        ContextUtils.prefetchResourceModels(fhirContext, JobQueueBatch.validResourceTypes);
    }

    @BeforeEach
    void setupEach() {
        queue = new MemoryBatchQueue(100);
        final var bbclient = Mockito.spy(new MockBlueButtonClient(fhirContext));
        engine = new AggregationEngine(aggregatorID, bbclient, queue, fhirContext, metricRegistry, operationsConfig);
        subscribe = Mockito.mock(Disposable.class);
        doReturn(false).when(subscribe).isDisposed();
        engine.setSubscribe(subscribe);
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void largeJobTestSingleResource() {
        // Make a simple job with one resource type
        final var orgID = UUID.randomUUID();
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.ExplanationOfBenefit)
        );

        // Do the job
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        final List<JobQueueBatchFile> sorted = completeJob.getJobQueueBatchFiles().stream().sorted(Comparator.comparingInt(JobQueueBatchFile::getSequence)).collect(Collectors.toList());
        assertAll(() -> assertEquals(4, sorted.size()),
                () -> assertEquals(10, sorted.get(0).getCount()),
                () -> assertEquals(2, sorted.get(3).getCount()));

        // Look at the output files
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.ExplanationOfBenefit, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void largeJobTest() {
        // Make a simple job with one resource type
        final var orgID = UUID.randomUUID();
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_MBIS,
                JobQueueBatch.validResourceTypes
        );

        // Do the job
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());

        // Look at the output files
        completeJob.getJobQueueBatchFiles()
                .forEach(batchFile -> {
                    final var outputFilePath = String.format("%s/%s.ndjson", exportPath, batchFile.getFileName());
                    final File file = new File(Path.of(outputFilePath).toString());
                    assertAll(() -> assertNotNull(file, "Should have input file"),
                            () -> assertArrayEquals(AggregationEngine.generateChecksum(file), batchFile.getChecksum(), "Should have checksum"),
                            () -> assertEquals(file.length(), batchFile.getFileLength(), "Should have matching file length"));
                });

        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void largeJobWithBadPatientTest() {
        // Make a simple job with one resource type
        final var orgID = UUID.randomUUID();
        final var jobID = queue.createJob(
                orgID,
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_WITH_BAD_IDS,
                Collections.singletonList(ResourceType.ExplanationOfBenefit)
        );

        // Do the job
        queue.claimBatch(engine.getAggregatorID())
                .ifPresent(engine::processJobBatch);

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        assertAll(
                () -> assertEquals(5, completeJob.getJobQueueBatchFiles().size(), String.format("Unexpected JobModel: %s", completeJob.toString())),
                () -> assertTrue(completeJob.getJobQueueFile(ResourceType.ExplanationOfBenefit).isPresent(), "Expect a EOB"),
                () -> assertTrue(completeJob.getJobQueueFile(ResourceType.OperationOutcome).isPresent(), "Expect an error"));

        // Look at the output files
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.ExplanationOfBenefit, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, completeJob.getBatchID(), ResourceType.OperationOutcome, 0);
        assertTrue(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }
}
