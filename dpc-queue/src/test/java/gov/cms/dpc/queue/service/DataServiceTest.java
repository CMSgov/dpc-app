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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataServiceTest {

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
    void before() {
        MockitoAnnotations.openMocks(this);
        dataService = new DataService(queue, fhirContext, exportPath, 1);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void after() {
        Mockito.reset(queue);
        if (tmpFile != null) {
            tmpFile.delete();
        }
    }

    @Test
    void whenGetJobBatchesThrowsException() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        Mockito.doThrow(new RuntimeException("error")).when(queue).getJobBatches(Mockito.any(UUID.class));

        assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType));
    }

    @Test
    void whenGetJobBatchesReturnsFailedJob() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(true, resourceType);
        assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType));
    }

    @Test
    void whenGetJobBatchesReturnsCompletedJobWithResourceType() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(false, resourceType);
        Resource resource = dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType);
        assertInstanceOf(Bundle.class, resource);
    }

    @Test
    void whenGetJobBatchesReturnsCompletedJobWithOperationOutcome() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(false, DPCResourceType.OperationOutcome);
        Resource resource = dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType);
        assertInstanceOf(OperationOutcome.class, resource);
    }

    @Test
    void whenPassingInNoResourceTypes() {
        workJob(false, DPCResourceType.ExplanationOfBenefit);
        assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString())));
    }

    @Test
    void whenQueueIsEmpty() {
        Mockito.when(queue.getJobBatches(Mockito.any(UUID.class))).thenReturn(List.of());

        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;
        assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType));
    }

    @Test
    void whenJobIsIncomplete() {
        Mockito.doAnswer(mock -> {
            Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
            while (workBatch.flatMap(batch -> batch.fetchNextPatient(aggregatorID)).isPresent()) {
                queue.completePartialBatch(workBatch.get(), aggregatorID);
            }
            return List.of(workBatch.get());
        }).when(queue).getJobBatches(Mockito.any());

        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;
        assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), resourceType));
    }

    @ParameterizedTest
    @EnumSource(value = DPCResourceType.class, names = {"Coverage", "ExplanationOfBenefit", "Patient"})
    void whenPassingInValidResourceTypes(DPCResourceType type) {
        workJob(false, type);
        assertDoesNotThrow(() -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), type));
    }

    @ParameterizedTest
    @EnumSource(value = DPCResourceType.class, names = {"Coverage", "ExplanationOfBenefit", "Patient", "OperationOutcome"}, mode = EnumSource.Mode.EXCLUDE)
    void whenPassingInInvalidResourceTypes(DPCResourceType type) {
        workJob(false, type);
        DataRetrievalException err = assertThrows(DataRetrievalException.class, () -> dataService.retrieveData(orgID, orgNPI, providerNPI, List.of(patientID.toString()), type));
        assertEquals("Unexpected resource type: " + type.name(), err.getMessage());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void workJob(boolean failBatch, DPCResourceType resourceType) {
        Mockito.doAnswer(mock -> {
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
