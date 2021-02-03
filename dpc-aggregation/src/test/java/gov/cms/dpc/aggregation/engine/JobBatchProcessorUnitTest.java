package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.service.ConsentResult;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.service.LookBackAnswer;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import org.assertj.core.util.Lists;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;

public class JobBatchProcessorUnitTest {

    @Mock
    BlueButtonClient mockBlueButtonClient;
    @Mock
    OperationsConfig mockOperationConfigs;
    @Mock
    MetricRegistry mockMetricRegistry;
    @Mock
    LookBackService mockLookBackService;
    @Mock
    ConsentService mockConsentService;
    @Mock
    IJobQueue mockJobQueue;

    private JobQueueBatch queueBatch;

    private JobBatchProcessor processorInTest;

    @BeforeEach
    public void setUp() throws GeneralSecurityException, NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        processorInTest = new JobBatchProcessor(mockBlueButtonClient, FhirContext.forDstu3(), mockMetricRegistry, mockOperationConfigs, mockLookBackService, mockConsentService);
        setupMocksForHappyPath();
    }

    private void setupMocksForHappyPath() throws GeneralSecurityException, NoSuchFieldException, IllegalAccessException {
        UUID orgIdUUID = UUID.randomUUID();
        String orgIdString = orgIdUUID.toString();

        UUID jobIdUUID = UUID.randomUUID();
        String jobIdString = jobIdUUID.toString();

        String practitionerNpi = "12345";

        String beneId = "ben123";

        //Setup Consent result
        ConsentResult consentResult = new ConsentResult();
        consentResult.setPolicyType(ConsentResult.PolicyType.OPT_IN);
        consentResult.setActive(true);
        consentResult.setConsentDate(new Date()); //TODO Check if date needs to be a few min before.
        Mockito.when(mockConsentService.getConsent(any())).thenReturn(Optional.of(Lists.list(consentResult)));

        //Setup look back exceptions
        Mockito.when(mockOperationConfigs.getLookBackExemptOrgs()).thenReturn(Lists.emptyList());

        LookBackAnswer answer = Mockito.mock(LookBackAnswer.class);
        Mockito.when(answer.matchDateCriteria()).thenReturn(false);
        Mockito.when(answer.orgNPIMatchAnyEobNPIs()).thenReturn(true);


        Mockito.when(mockLookBackService.getPractitionerNPIFromRoster(any(),any(),any())).thenReturn(practitionerNpi);
        Mockito.when(mockLookBackService.getLookBackAnswer(any(),any(),any(),anyLong())).thenReturn(answer);
        queueBatch = new JobQueueBatch(jobIdUUID,
                orgIdUUID,
                "providerId",
                Lists.list("mbi1"),
                Lists.list(ResourceType.ExplanationOfBenefit, ResourceType.Patient, ResourceType.Coverage),
                null,
                OffsetDateTime.now(), "0.0.0",true);

        Bundle mockPatientBundle = Mockito.mock(Bundle.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockBlueButtonClient.requestPatientFromServerByMbi(eq("mbi1"), any())).thenReturn(mockPatientBundle);
        Mockito.when(mockPatientBundle.getTotal()).thenReturn(1);
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.BENE_ID.getSystem()).setValue(beneId);
        Mockito.when(mockPatientBundle.getEntryFirstRep().getResource()).thenReturn(patient);
        Bundle mockPatientsBundle = Mockito.mock(Bundle.class);
        Bundle mockEobBundle = Mockito.mock(Bundle.class);
        Bundle mockCoverageBundle = Mockito.mock(Bundle.class);

        Mockito.when(mockBlueButtonClient.requestPatientFromServer(eq(beneId),any(),any())).thenReturn(mockPatientsBundle);
        Mockito.when(mockBlueButtonClient.requestEOBFromServer(eq(beneId),any(),any())).thenReturn(mockEobBundle);
        Mockito.when(mockBlueButtonClient.requestCoverageFromServer(eq(beneId),any(),any())).thenReturn(mockCoverageBundle);


        //Configs
        Mockito.when(mockOperationConfigs.getExportPath()).thenReturn("/tmp");
        Mockito.when(mockOperationConfigs.getResourcesPerFileCount()).thenReturn(100);
        Field resourceMeterField = JobBatchProcessor.class.getDeclaredField("resourceMeter");
        resourceMeterField.setAccessible(true);
        resourceMeterField.set(processorInTest, Mockito.mock(Meter.class));

        Field operationOutcomeMeterField = JobBatchProcessor.class.getDeclaredField("operationalOutcomeMeter");
        operationOutcomeMeterField.setAccessible(true);
        operationOutcomeMeterField.set(processorInTest, Mockito.mock(Meter.class));
    }

    @Test
    public void testProcessJobBatchPartialHappyPath() {
        processorInTest.processJobBatchPartial(UUID.randomUUID(),mockJobQueue,queueBatch,"mbi1");
        Mockito.verify(mockJobQueue, Mockito.times(1)).completePartialBatch(any(),any());
        //TODO verify the actual output.
    }
}