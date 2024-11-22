package gov.cms.dpc.queue.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.exceptions.DataRetrievalException;
import gov.cms.dpc.queue.models.JobQueueBatch;
import org.assertj.core.util.Files;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Job Queue data service responses")
public class DataServiceTest {

    private final UUID aggregatorID = UUID.randomUUID();
    private final UUID orgID = UUID.randomUUID();
    private final UUID patientID = UUID.randomUUID();
    private final String orgNPI = NPIUtil.generateNPI();
    private final String providerNPI = NPIUtil.generateNPI();
    private final String exportPath = "/tmp";
    private DataService dataService;
    private File tmpFile;

    @Spy
    private MemoryBatchQueue queue;

    @Spy
    private FhirContext fhirContext;

    @BeforeEach
    public void before() {
        MockitoAnnotations.openMocks(this);
        dataService = new DataService(queue, fhirContext, exportPath, 1);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    public void after() {
        Mockito.reset(queue);
        if (tmpFile != null) {
            tmpFile.delete();
        }
    }

    @Test
    @DisplayName("Handle data retrieval exceptions during job 🤮")
    public void whenGetJobBatchesThrowsException() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        Mockito.doThrow(new RuntimeException("error")).when(queue).getJobBatches(Mockito.any(UUID.class));

        Assertions.assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType));
    }

    @Test
    @DisplayName("Handle failed jobs 🤮")
    public void whenGetJobBatchesReturnsFailedJob() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(true, resourceType);
        Assertions.assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType));
    }

    @Test
    @DisplayName("Get job batch with completed job as resource bundle 🥳")
    public void whenGetJobBatchesReturnsCompletedJobWithResourceType() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(false, resourceType);
        Resource resource = dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType);
        Assertions.assertTrue(resource instanceof Bundle);
    }

    @Test
    @DisplayName("Get job batch with completed job as outcome 🥳")
    public void whenGetJobBatchesReturnsCompletedJobWithOperationOutcome() {
        UUID randomPatientID = UUID.randomUUID();
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(false, DPCResourceType.OperationOutcome);
        Resource resource = dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(randomPatientID.toString()), resourceType);
        Assertions.assertTrue(resource instanceof OperationOutcome);
    }

    @Test
    @DisplayName("Handle exception when providing no resource type to retrieve data 🥳")
    public void whenPassingInNoResourceTypes() {
        workJob(false, DPCResourceType.ExplanationOfBenefit);
        Assertions.assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString())));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void workJob(boolean failBatch, DPCResourceType resourceType) {
        Mockito.doAnswer((mock) -> {
            Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
            while (workBatch.flatMap(batch -> batch.fetchNextPatient(aggregatorID)).isPresent()) {
                queue.completePartialBatch(workBatch.get(), aggregatorID);
            }
            if (failBatch) {
                queue.failBatch(workBatch.get(), aggregatorID);
            } else {
                tmpFile = Files.newFile(String.format("%s/%s-%s.%s.ndjson", exportPath, workBatch.get().getBatchID().toString(), 0, resourceType.getPath()));
                workBatch.get().addJobQueueFile(resourceType, 0, 1);
                queue.completeBatch(workBatch.get(), aggregatorID);
            }
            return List.of(workBatch.get());
        }).when(queue).getJobBatches(Mockito.any());
    }
}
