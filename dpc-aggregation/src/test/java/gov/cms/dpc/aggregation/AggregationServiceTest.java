package gov.cms.dpc.aggregation;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.DistributedBatchQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
@ExtendWith(BufferedLoggerHandler.class)
public class AggregationServiceTest {

    private static final String configPath = "src/test/resources/test.application.yml";
    private static final DropwizardTestSupport<DPCAggregationConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCAggregationService.class, configPath,
                    ConfigOverride.config("server.applicationConnectors[0].port", "7777"),
                    ConfigOverride.config("server.adminConnectors[0].port", "8888"));

	private DistributedBatchQueue queue;

    @BeforeEach
    void start() throws Exception{
        final Configuration conf = new Configuration();
		SessionFactory sessionFactory = conf.configure().buildSessionFactory();
        queue = new DistributedBatchQueue(new DPCQueueManagedSessionFactory(sessionFactory), 100, new MetricRegistry());

        APPLICATION.before();
    }

    @AfterEach
    void stop() {
        APPLICATION.after();
    }

    @Test
    void testHealthChecks() {
        final HealthCheckRegistry checks = APPLICATION.getEnvironment().healthChecks();
        final SortedSet<String> names = checks.getNames();

        // Ensure that the various healthchecks are propagated from the modules
        assertAll(() -> assertTrue(names.contains("blue-button-client"), "Should have BB health check"));
        assertAll(() -> assertTrue(names.contains("aggregation-engine"), "Should have Aggregation Engine health check"));
        assertAll(() -> assertTrue(names.contains("dpc-consent"), "Should have dpc-consent health check"));

        // Everything should be true
        checks.runHealthChecks().forEach((key, value) -> assertTrue(value.isHealthy(), String.format("Healthcheck: %s is not ok.", key)));
    }

    @Test
    void testStoppingEngineMidBatch() throws InterruptedException {
        // Create a batch that will hang forever
        final var orgID = UUID.randomUUID();
        final List<String> mbis = List.of(MockBlueButtonClient.TEST_PATIENT_TIME_OUT);

        final var jobID = queue.createJob(
            orgID,
            NPIUtil.generateNPI(),
            NPIUtil.generateNPI(),
            mbis,
            Collections.singletonList(DPCResourceType.Patient),
            null,
            MockBlueButtonClient.BFD_TRANSACTION_TIME,
            null, null, true, false);

        // Wait for dpc-aggregation to pick up the batch, set status to RUNNING and hang on getting the patient from BFD
        await().until(() -> {
            JobQueueBatch batch = queue.getJobBatches(jobID).get(0);
            return batch.getStatus() == JobStatus.RUNNING;
        });

        // Stop aggregation and make sure it pauses the batch
        APPLICATION.after();
        assertEquals(JobStatus.QUEUED, queue.getJobBatches(jobID).get(0).getStatus());
    }
}
