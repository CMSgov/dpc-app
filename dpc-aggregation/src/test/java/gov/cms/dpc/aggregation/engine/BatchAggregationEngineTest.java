package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BatchAggregationEngineTest {
    private static final String TEST_PROVIDER_ID = "1";
    private JobQueue queue;
    private AggregationEngine engine;

    static private FhirContext fhirContext = FhirContext.forDstu3();
    static private MetricRegistry metricRegistry = new MetricRegistry();
    static private String exportPath;
    static private OperationsConfig operationsConfig;

    @BeforeAll
    static void setupAll() {
        fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
        final var config = ConfigFactory.load("test.application.conf").getConfig("dpc.aggregation");
        exportPath = config.getString("exportPath");
        operationsConfig = new OperationsConfig(10, exportPath);
        AggregationEngine.setGlobalErrorHandler();

        // Force HAPI scanning early
        fhirContext.getResourceDefinition("Patient");
        fhirContext.getResourceDefinition("Bundle");
        fhirContext.getResourceDefinition("ExplanationOfBenefit");
    }

    @BeforeEach
    void setupEach() {
        queue = new MemoryQueue();
        final var bbclient = Mockito.spy(new MockBlueButtonClient(fhirContext));
        engine = new AggregationEngine(bbclient, queue, fhirContext, metricRegistry, operationsConfig);
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void largeJobTestSingleResource() {
        // Make a simple job with one resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                Collections.singletonList(ResourceType.ExplanationOfBenefit),
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_IDS.get(0)));

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair));

        // Look at the result
        final var completeJob = queue.getJob(jobId).orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        final List<JobResult> sorted = completeJob.getJobResults().stream().sorted(Comparator.comparingInt(JobResult::getSequence)).collect(Collectors.toList());
        assertAll(() -> assertEquals(4, sorted.size()),
                () -> assertEquals(10, sorted.get(0).getCount()),
                () -> assertEquals(2, sorted.get(3).getCount()));

        // Look at the output files
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, jobId, ResourceType.ExplanationOfBenefit, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, jobId, ResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void largeJobTest() {
        // Make a simple job with one resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_IDS);

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair));

        // Look at the result
        final var completeJob = queue.getJob(jobId).orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());

        // Look at the output files
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, jobId, ResourceType.ExplanationOfBenefit, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, jobId, ResourceType.OperationOutcome, 0);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if a engine can handle a simple job with one resource type, one test provider, and one patient.
     */
    @Test
    void largeJobWithBadPatientTest() {
        // Make a simple job with one resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                Collections.singletonList(ResourceType.ExplanationOfBenefit),
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_WITH_BAD_IDS);

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair));

        // Look at the result
        final var completeJob = queue.getJob(jobId).orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        assertAll(
                () -> assertEquals(5, completeJob.getJobResults().size(), String.format("Unexpected JobModel: %s", completeJob.toString())),
                () -> assertTrue(completeJob.getJobResult(ResourceType.ExplanationOfBenefit).isPresent(), "Expect a EOB"),
                () -> assertTrue(completeJob.getJobResult(ResourceType.OperationOutcome).isPresent(), "Expect an error"));

        // Look at the output files
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, jobId, ResourceType.ExplanationOfBenefit, 0);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, jobId, ResourceType.OperationOutcome, 0);
        assertTrue(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }
}
