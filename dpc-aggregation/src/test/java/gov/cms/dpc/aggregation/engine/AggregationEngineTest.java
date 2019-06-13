package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import io.github.resilience4j.retry.RetryConfig;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
    private JobQueue queue;
    private AggregationEngine engine;

    static private Config config;

    @BeforeAll
    static void setupAll() {
        config = ConfigFactory.load("test.application.conf").getConfig("dpc.aggregation");
    }

    @BeforeEach
    void setupEach() {
        queue = new MemoryQueue();
        bbclient = Mockito.spy(new MockBlueButtonClient());
        engine = new AggregationEngine(bbclient, queue, FhirContext.forDstu3(), config.getString("exportPath"), config, RetryConfig.ofDefaults());
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
        // Make a simple job with one resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                Collections.singletonList(ResourceType.Patient),
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_IDS.get(0)));

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        final var completeJob = queue.getJob(jobId).orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());
        final var outputFilePath = engine.formOutputFilePath(jobId, ResourceType.Patient);
        assertTrue(Files.exists(Path.of(outputFilePath)));
        final var errorFilePath = engine.formOutputFilePath(jobId, ResourceType.OperationOutcome);
        assertFalse(Files.exists(Path.of(errorFilePath)), "expect no error file");
    }

    /**
     * Test if the engine can handle a job with multiple output files and patients
     */
    @Test
    void multipleFileJobTest() {
        // build a job with multiple resource types
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                JobModel.validResourceTypes,
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_IDS);

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        assertAll(() -> assertTrue(queue.getJob(jobId).isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJob(jobId).get().getStatus()));
        JobModel.validResourceTypes.forEach(resourceType -> {
            var outputFilePath = engine.formOutputFilePath(jobId, resourceType);
            assertTrue(Files.exists(Path.of(outputFilePath)));
        });
    }

    /**
     * Test if the engine can handle a job with no attributions
     */
    @Test
    void emptyJobTest() {
        // Job with a unsupported resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                List.of(ResourceType.Patient),
                TEST_PROVIDER_ID,
                List.of());

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        assertFalse(queue.getJob(jobId).isEmpty(), "Unable to retrieve job from queue.");
        queue.getJob(jobId).ifPresent(retrievedJob -> {
            assertEquals(JobStatus.COMPLETED, retrievedJob.getStatus());
            assertEquals(0, retrievedJob.getJobResults().size());
            assertFalse(Files.exists(Path.of(engine.formOutputFilePath(jobId, ResourceType.Patient))));
            assertFalse(Files.exists(Path.of(engine.formOutputFilePath(jobId, ResourceType.OperationOutcome))));
        });
    }


    /**
     * Test if the engine can handle a job with bad parameters
     */
    @Test
    void badJobTest() {
        // Job with a unsupported resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                List.of(ResourceType.Schedule),
                TEST_PROVIDER_ID,
                MockBlueButtonClient.TEST_PATIENT_IDS);

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        assertAll(() -> assertTrue(queue.getJob(jobId).isPresent(), "Unable to retrieve job from queue."),
                () -> assertEquals(JobStatus.FAILED, queue.getJob(jobId).get().getStatus()));
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

        final var jobID = UUID.randomUUID();
        JobModel job = new JobModel(jobID,
                List.of(ResourceType.ExplanationOfBenefit, ResourceType.Patient),
                TEST_PROVIDER_ID,
                patientIDs);

        // Do the job
        queue.submitJob(jobID, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        assertAll(() -> assertTrue(queue.getJob(jobID).isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJob(jobID).get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServer(idCaptor.capture());
        Mockito.verify(bbclient, atLeastOnce()).requestEOBFromServer(idCaptor.capture());
        var values = idCaptor.getAllValues();
        assertEquals(6,
                idCaptor.getAllValues().stream().filter(value -> value.equals("-1")).count(),
                "Should be 6 invalid ids, 3 retries per method x 2 method calls x 1 bad-id");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJob(jobID).isPresent());
        final var actual = queue.getJob(jobID).get();
        var expectedErrorPath = engine.formOutputFilePath(jobID, ResourceType.OperationOutcome);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(3, actual.getJobResults().size(), "expected 3 (= 2 output + 1 error) resource types"),
                () -> assertEquals(2, actual.getJobResult(ResourceType.OperationOutcome).orElseThrow().getCount(), "expected 2 (= 1 bad patient x 2 resource types)"),
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

        final var jobID = UUID.randomUUID();
        JobModel job = new JobModel(jobID,
                Collections.singletonList(ResourceType.Patient),
                TEST_PROVIDER_ID,
                Collections.singletonList("1"));

        // Do the job
        queue.submitJob(jobID, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        assertAll(() -> assertTrue(queue.getJob(jobID).isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJob(jobID).get().getStatus()));

        // Check that the bad ID was called 3 times
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(bbclient, atLeastOnce()).requestPatientFromServer(idCaptor.capture());
        assertEquals(3, idCaptor.getAllValues().stream().filter(value -> value.equals("1")).count(), "Should have been called 3 times to get the patient, but with errors instead");

        // Look at the result. It should have one error, but be successful otherwise.
        assertTrue(queue.getJob(jobID).isPresent());
        final var actual = queue.getJob(jobID).get();
        var expectedErrorPath = engine.formOutputFilePath(jobID, ResourceType.OperationOutcome);
        assertAll(() -> assertEquals(JobStatus.COMPLETED, actual.getStatus()),
                () -> assertEquals(1, actual.getJobResults().size(), "expected just a operational outcome"),
                () -> assertEquals(1, actual.getJobResult(ResourceType.OperationOutcome).orElseThrow().getCount(), "expected 1 bad patient fetch"),
                () -> assertTrue(Files.exists(Path.of(expectedErrorPath)), "expected an error file"));
    }
}