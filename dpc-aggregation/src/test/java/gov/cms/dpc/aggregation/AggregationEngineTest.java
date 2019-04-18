package gov.cms.dpc.aggregation;

import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.aggregation.bbclient.MockBlueButtonClient;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

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
        bbclient = new MockBlueButtonClient();
        engine = new AggregationEngine(bbclient, queue, config);
    }

    @Test
    void mockBlueButtonClientTest() {
        Patient patient = bbclient.requestPatientFromServer(MockBlueButtonClient.TEST_PATIENT_IDS[0]);
        assertNotNull(patient);
    }

    @Test
    void simpleJobTest() {
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId, List.of(ResourceType.Patient), TEST_PROVIDER_ID, List.of(MockBlueButtonClient.TEST_PATIENT_IDS[0]));
        queue.submitJob(jobId, job);
        engine.workQueue();

        // Look at the result
        assertEquals(queue.getJobStatus(jobId), Optional.of(JobStatus.COMPLETED));
        var outputFilePath = engine.formOutputFilePath(jobId, ResourceType.Patient);
        assertTrue(Files.exists(Path.of(outputFilePath)));
    }

    @Test
    void multipleFileJobTest() {
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId, JobModel.validResourceTypes, TEST_PROVIDER_ID, List.of(MockBlueButtonClient.TEST_PATIENT_IDS));
        queue.submitJob(jobId, job);
        engine.workQueue();

        // Look at the result
        assertEquals(queue.getJobStatus(jobId), Optional.of(JobStatus.COMPLETED));
        JobModel.validResourceTypes.stream().forEach( resourceType -> {
            var outputFilePath = engine.formOutputFilePath(jobId, resourceType);
            assertTrue(Files.exists(Path.of(outputFilePath)));
        });
    }

    @Test
    void badJobTest() {
        // Test with a unsupported resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId, List.of(ResourceType.Schedule), TEST_PROVIDER_ID, List.of(MockBlueButtonClient.TEST_PATIENT_IDS));
        queue.submitJob(jobId, job);
        engine.workQueue();

        // Look at the result
        assertEquals(queue.getJobStatus(jobId), Optional.of(JobStatus.FAILED));
    }

}