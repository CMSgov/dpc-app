package gov.cms.dpc.aggregation.health;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.engine.JobBatchProcessor;
import gov.cms.dpc.aggregation.engine.JobBatchProcessorV2;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.service.LookBackServiceImpl;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.bluebutton.clientV2.BlueButtonClientV2;
import gov.cms.dpc.bluebutton.clientV2.MockBlueButtonClientV2;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.time.YearMonth;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;

/**
 * These tests are here to make sure the engine is still running/polling in situations where errors are recoverable.
 * A test to check if the engine exits out of the loop correctly when an error occurs
 * in AggregationEngineTest#testUnhealthyIfProcessJobBatchThrowsException
 */
@ExtendWith(BufferedLoggerHandler.class)
public class AggregationEngineHealthCheckTest {
    private static final String TEST_ORG_NPI = NPIUtil.generateNPI();
    private static final String TEST_PROVIDER_NPI = NPIUtil.generateNPI();
    private static final UUID aggregatorID = UUID.randomUUID();

    private IJobQueue queue;
    private BlueButtonClient bbclient;
    private BlueButtonClientV2 bbclientV2;
    private AggregationEngine engine;
    private ConsentService consentService;

    static private final FhirContext fhirContext = FhirContext.forDstu3();
    static private final FhirContext fhirContextR4 = FhirContext.forR4();
    static private final MetricRegistry metricRegistry = new MetricRegistry();
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
        bbclientV2 = Mockito.spy(new MockBlueButtonClientV2(fhirContextR4));
        var operationalConfig = new OperationsConfig(1000, exportPath, 500, YearMonth.of(2015, 3));
        LookBackServiceImpl lookBackService = Mockito.spy(new LookBackServiceImpl(operationalConfig));
        JobBatchProcessor jobBatchProcessor = Mockito.spy(new JobBatchProcessor(bbclient, fhirContext, metricRegistry, operationalConfig, lookBackService, consentService));
        JobBatchProcessorV2 jobBatchProcessorV2 = Mockito.spy(new JobBatchProcessorV2(bbclientV2, fhirContextR4, metricRegistry, operationalConfig, consentService));
        engine = Mockito.spy(new AggregationEngine(aggregatorID, queue, operationalConfig, jobBatchProcessor, jobBatchProcessorV2));
        AggregationEngine.setGlobalErrorHandler();
    }

    @Test
    public void testHealthyEngine() throws InterruptedException {

        final var orgID = UUID.randomUUID();

        queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList("1"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        AggregationEngineHealthCheck healthCheck = new AggregationEngineHealthCheck(engine);
        assertTrue(healthCheck.check().isHealthy());

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(engine);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(healthCheck.check().isHealthy());

    }

    @Test
    public void testHealthyEngineWhenJobBatchErrors() throws InterruptedException {

        Mockito.doThrow(new RuntimeException("Error")).when(bbclient).requestPatientFromServer(Mockito.anyString(), Mockito.any(DateRangeParam.class), anyMap());

        final var orgID = UUID.randomUUID();

        queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList("1"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        AggregationEngineHealthCheck healthCheck = new AggregationEngineHealthCheck(engine);
        assertTrue(healthCheck.check().isHealthy());

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(engine);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(healthCheck.check().isHealthy());

    }

    @Test
    public void testHealthyEngineWhenClaimBatchErrors() throws InterruptedException {

        final var orgID = UUID.randomUUID();

        queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList("1"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        Mockito.doThrow(new RuntimeException("Error")).when(queue).claimBatch(Mockito.any(UUID.class));

        AggregationEngineHealthCheck healthCheck = new AggregationEngineHealthCheck(engine);
        assertTrue(healthCheck.check().isHealthy());

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(engine);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(healthCheck.check().isHealthy());

    }

    @Test
    public void testHealthyEngineWhenQueueOperationsError() throws InterruptedException {
        Mockito.doThrow(new RuntimeException("Error")).when(queue).completePartialBatch(Mockito.any(JobQueueBatch.class), Mockito.any(UUID.class));

        final var orgID = UUID.randomUUID();

        queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList("1"),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false);

        AggregationEngineHealthCheck healthCheck = new AggregationEngineHealthCheck(engine);
        assertTrue(healthCheck.check().isHealthy());

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(engine);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(healthCheck.check().isHealthy());
    }
}
