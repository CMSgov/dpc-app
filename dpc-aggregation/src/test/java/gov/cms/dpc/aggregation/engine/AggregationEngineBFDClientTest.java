package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import com.codahale.metrics.MetricRegistry;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.aggregation.service.ConsentResult;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.BlueButtonClientImpl;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(BufferedLoggerHandler.class)
public class AggregationEngineBFDClientTest {

    @TempDir
    Path tempDir;

    private static final FhirContext fhirContext = FhirContext.forDstu3();
    private static final MetricRegistry metricRegistry = new MetricRegistry();

    private final IGenericClient bbClient = Mockito.mock(IGenericClient.class);
    private final LookBackService lookBackService = Mockito.mock(LookBackService.class);
    private final ConsentService mockConsentService = Mockito.mock(ConsentService.class);

    private AggregationEngine engine;
    private IJobQueue queue;
    private final UUID orgID = UUID.randomUUID();
    private final String TEST_ORG_NPI = NPIUtil.generateNPI();
    private final String TEST_PROVIDER_NPI = NPIUtil.generateNPI();

    @BeforeEach
    public void setup() throws GeneralSecurityException {
        BlueButtonClient blueButtonClient = Mockito.spy(new BlueButtonClientImpl(bbClient, new BBClientConfiguration(), metricRegistry));
        OperationsConfig config = new OperationsConfig(1000, tempDir.toString(), 1, 1, 1, YearMonth.now(), List.of(orgID.toString()));
        JobBatchProcessor processor = new JobBatchProcessor(blueButtonClient, fhirContext, metricRegistry, config, lookBackService, mockConsentService);
        queue = new MemoryBatchQueue(100);
        engine = new AggregationEngine(UUID.randomUUID(), queue, config, processor);
        engine.queueRunning.set(true);

        Mockito.when(blueButtonClient.hashMbi(Mockito.anyString())).thenReturn(MockBlueButtonClient.MBI_HASH_MAP.get(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)));
        ConsentResult consentResult = new ConsentResult();
        consentResult.setConsentDate(new Date());
        consentResult.setActive(true);
        consentResult.setPolicyType(ConsentResult.PolicyType.OPT_IN);
        consentResult.setConsentId(UUID.randomUUID().toString());
        Mockito.when(mockConsentService.getConsent(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0))).thenReturn(Optional.of(Lists.list(consentResult)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHeadersPassedToBFDForBulkJob() {
        //Mock out the interactions of using IGenericClient to capture things
        IUntypedQuery<IBaseBundle> iUntypedQuery = Mockito.mock(IUntypedQuery.class);
        Mockito.when(bbClient.search()).thenReturn(iUntypedQuery);
        IQuery<IBaseBundle> iQuery = Mockito.mock(IQuery.class);
        Mockito.when(iUntypedQuery.forResource(Patient.class)).thenReturn(iQuery);
        Mockito.when(iQuery.where(Mockito.any(ICriterion.class))).thenReturn(iQuery);
        ArgumentCaptor<String> headerKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        Mockito.when(iQuery.withAdditionalHeader(headerKey.capture(), headerValue.capture())).thenReturn(iQuery);

        UUID providerID = UUID.randomUUID();
        UUID jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                "127.0.0.1",
                null,
                true);

        engine.run();

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());

        Assertions.assertThat(headerKey.getAllValues()).containsExactlyInAnyOrder(Constants.INCLUDE_IDENTIFIERS_HEADER, Constants.BULK_CLIENT_ID_HEADER, Constants.BULK_JOB_ID_HEADER, HttpHeaders.X_FORWARDED_FOR, Constants.BFD_ORIGINAL_QUERY_ID_HEADER);
        Assertions.assertThat(headerValue.getAllValues()).containsExactlyInAnyOrder("mbi", TEST_PROVIDER_NPI, jobID.toString(), "127.0.0.1", jobID.toString());

        engine.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHeadersPassedToBFDForNonBulkJob() {
        //Mock out the interactions of using IGenericClient to capture things
        IUntypedQuery<IBaseBundle> iUntypedQuery = Mockito.mock(IUntypedQuery.class);
        Mockito.when(bbClient.search()).thenReturn(iUntypedQuery);
        IQuery<IBaseBundle> iQuery = Mockito.mock(IQuery.class);
        Mockito.when(iUntypedQuery.forResource(Patient.class)).thenReturn(iQuery);
        Mockito.when(iQuery.where(Mockito.any(ICriterion.class))).thenReturn(iQuery);
        ArgumentCaptor<String> headerKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        Mockito.when(iQuery.withAdditionalHeader(headerKey.capture(), headerValue.capture())).thenReturn(iQuery);

        UUID providerID = UUID.randomUUID();
        UUID jobID = queue.createJob(
                orgID,
                TEST_ORG_NPI,
                TEST_PROVIDER_NPI,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)),
                Collections.singletonList(ResourceType.Patient),
                null,
                MockBlueButtonClient.BFD_TRANSACTION_TIME,
                "127.0.0.1",
                null,
                false);

        engine.run();

        // Look at the result
        final var completeJob = queue.getJobBatches(jobID).stream().findFirst().orElseThrow();
        assertEquals(JobStatus.COMPLETED, completeJob.getStatus());

        Assertions.assertThat(headerKey.getAllValues()).containsExactlyInAnyOrder(Constants.INCLUDE_IDENTIFIERS_HEADER, Constants.DPC_CLIENT_ID_HEADER, HttpHeaders.X_FORWARDED_FOR, Constants.BFD_ORIGINAL_QUERY_ID_HEADER);
        Assertions.assertThat(headerValue.getAllValues()).containsExactlyInAnyOrder("mbi", TEST_PROVIDER_NPI, "127.0.0.1", jobID.toString());

        engine.stop();
    }
}
