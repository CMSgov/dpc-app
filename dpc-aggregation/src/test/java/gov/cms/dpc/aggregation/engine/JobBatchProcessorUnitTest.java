package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.service.ConsentResult;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.service.EveryoneGetsDataLookBackServiceImpl;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class JobBatchProcessorUnitTest {
    private static final String exportPath = "/tmp";
    private static final String TEST_ORG_NPI = NPIUtil.generateNPI();
    private static final String TEST_PROVIDER_NPI = NPIUtil.generateNPI();
    private static ConsentResult optIn;
    private static ConsentResult optOut;

    private final MetricRegistry metricRegistry = new MetricRegistry();

    @Spy
    private BlueButtonClient bbClient = new MockBlueButtonClient(FhirContext.forDstu3());
    @Mock
    private ConsentService consentService;

    @BeforeAll
    static void setup() {
        optIn = new ConsentResult();
        optIn.setConsentDate(new Date());
        optIn.setActive(true);
        optIn.setPolicyType(ConsentResult.PolicyType.OPT_IN);
        optIn.setConsentId(UUID.randomUUID().toString());

        optOut = new ConsentResult();
        optOut.setConsentDate(new Date());
        optOut.setActive(true);
        optOut.setPolicyType(ConsentResult.PolicyType.OPT_OUT);
        optOut.setConsentId(UUID.randomUUID().toString());
    }

    @Test
    public void testHappyPath() {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenReturn(Optional.of(List.of(optIn)));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertNoError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testHappyPath_lookBackExempt() {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);
        JobQueueBatch job = jobs.get(0);

        // Create a config with our org look back exempt
        OperationsConfig operationsConfig = new OperationsConfig(
                1000,
                exportPath,
                1,
                500,
                120,
                YearMonth.of(2014, 3),
                List.of(job.getOrgID().toString())
        );
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenReturn(Optional.of(List.of(optIn)));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                job,
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertNoError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testHappyPath_MoreThanOnePatientInJob() {
        List<String> mbis = List.of(
                MockBlueButtonClient.TEST_PATIENT_MBIS.get(0),
                MockBlueButtonClient.TEST_PATIENT_MBIS.get(1)
                );

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                mbis,
                List.of(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);
        JobQueueBatch job = jobs.get(0);

        // Create a config with our org look back exempt
        OperationsConfig operationsConfig = new OperationsConfig(
                1,
                exportPath,
                1,
                500,
                120,
                YearMonth.of(2014, 3),
                List.of(job.getOrgID().toString())
        );
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        Mockito.when(consentService.getConsent(List.of(mbis.get(0)))).thenReturn(Optional.of(List.of(optIn)));
        Mockito.when(consentService.getConsent(List.of(mbis.get(1)))).thenReturn(Optional.of(List.of(optIn)));

        List<JobQueueBatchFile> results1 = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                job,
                mbis.get(0)
        );

        List<JobQueueBatchFile> results2 = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                job,
                mbis.get(1)
        );


        assertEquals(1, results1.size());
        JobQueueBatchFile completedJob1 = results1.get(0);
        assertNoError(completedJob1.getBatchID(), DPCResourceType.Patient);

        assertEquals(1, results2.size());
        JobQueueBatchFile completedJob2 = results1.get(0);
        assertNoError(completedJob2.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testHappyPath_NoConsent() {
        // No consent records gets treated like an opt in by the JobBatchProcessor

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenReturn(Optional.of(List.of()));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertNoError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testError_LoadingPatientByMbi() throws GeneralSecurityException {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        Mockito.doThrow(new IllegalStateException("bad mbi test")).when(bbClient).requestPatientFromServerByMbi(eq(mbi), any());

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testError_MultiplePatientsForMbi() throws GeneralSecurityException {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        // Bundle with multiple patients
        Bundle bundle = new Bundle();
        bundle.setTotal(2);

        Mockito.doReturn(bundle).when(bbClient).requestPatientFromServerByMbi(eq(mbi), any());

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testError_ConsentServiceException() {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenThrow(new IllegalStateException("consent error"));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testPatientOptOut() {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenReturn(Optional.of(List.of(optOut)));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testFailsLookBackCheck() {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);
        String id = MockBlueButtonClient.MBI_BENE_ID_MAP.get(mbi);

        Bundle emptyBundle = new Bundle();
        Mockito.doReturn(emptyBundle).when(bbClient).requestEOBFromServer(eq(id), any(), any());

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenReturn(Optional.of(List.of(optIn)));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    @Test
    public void testError_NoPractitionerAndOrgLookBack() {
        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);

        OperationsConfig operationsConfig = getOperationsConfig();
        JobBatchProcessor jobBatchProcessor = getJobBatchProcessor(bbClient, operationsConfig, new EveryoneGetsDataLookBackServiceImpl(), consentService);

        IJobQueue queue = new MemoryBatchQueue();
        final var jobID = queue.createJob(
                UUID.randomUUID(),
                null,
                null,
                Collections.singletonList(mbi),
                Collections.singletonList(DPCResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                null, null, true, false
        );
        List<JobQueueBatch> jobs = queue.getJobBatches(jobID);

        Mockito.when(consentService.getConsent(List.of(mbi))).thenReturn(Optional.of(List.of(optIn)));

        List<JobQueueBatchFile> results = jobBatchProcessor.processJobBatchPartial(
                UUID.randomUUID(),
                queue,
                jobs.get(0),
                mbi
        );

        assertEquals(1, results.size());
        JobQueueBatchFile completedJob = results.get(0);

        assertError(completedJob.getBatchID(), DPCResourceType.Patient);
    }

    private JobBatchProcessor getJobBatchProcessor(BlueButtonClient bbClient, OperationsConfig config, LookBackService lookBackSrvc, ConsentService consentSrvc) {
        return new JobBatchProcessor(
                bbClient,
                FhirContext.forDstu3(),
                metricRegistry,
                config,
                lookBackSrvc,
                consentSrvc
        );
    }

    // Creates a generic config
    private OperationsConfig getOperationsConfig() {
        return new OperationsConfig(
                1000,
                exportPath,
                500,
                YearMonth.of(2014, 3)
        );
    }

    private void assertError(UUID batchId, DPCResourceType resourceType) {
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, batchId, resourceType, 0);
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, batchId, DPCResourceType.OperationOutcome, 0);

        // Error file exists, but not output file
        assertFalse(Files.exists(Path.of(outputFilePath)));
        assertTrue(Files.exists(Path.of(errorFilePath)));
    }

    private void assertNoError(UUID batchId, DPCResourceType resourceType) {
        final var outputFilePath = ResourceWriter.formOutputFilePath(exportPath, batchId, resourceType, 0);
        final var errorFilePath = ResourceWriter.formOutputFilePath(exportPath, batchId, DPCResourceType.OperationOutcome, 0);

        // Output file exists, but no error file
        assertTrue(Files.exists(Path.of(outputFilePath)));
        assertFalse(Files.exists(Path.of(errorFilePath)));
    }
}
