package gov.cms.dpc.queue.service;

import ca.uhn.fhir.context.FhirContext;
import edu.emory.mathcs.backport.java.util.Collections;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.exceptions.DataRetrievalException;
import gov.cms.dpc.queue.models.JobQueueBatch;
import org.assertj.core.util.Files;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public class DataServiceTest{

    private final UUID aggregatorID = UUID.randomUUID();
    private final String exportPath = "/tmp";
    private DataService dataService;
    private File tmpFile;

    @Spy
    private MemoryBatchQueue queue;

    @Spy
    private FhirContext fhirContext;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        dataService = new DataService(queue, fhirContext, exportPath, 1);
    }

    @AfterEach
    public void after() {
        Mockito.reset(queue);
        if (tmpFile != null) {
            tmpFile.delete();
        }
    }

    @Test
    public void whenGetJobBatchesThrowsException() {
        UUID orgID = UUID.randomUUID();
        UUID providerID = UUID.randomUUID();
        UUID patientID = UUID.randomUUID();
        ResourceType resourceType = ResourceType.ExplanationOfBenefit;

        Mockito.doThrow(new RuntimeException("error")).when(queue).getJobBatches(Mockito.any(UUID.class));

        Assertions.assertThrows(DataRetrievalException.class, () -> {
            dataService.retrieveData(orgID, providerID, Collections.singletonList(patientID.toString()), null, OffsetDateTime.now(ZoneOffset.UTC), resourceType);
        });
    }

    @Test
    public void whenGetJobBatchesReturnsFailedJob() throws IllegalAccessException {
        UUID orgID = UUID.randomUUID();
        UUID providerID = UUID.randomUUID();
        UUID patientID = UUID.randomUUID();
        ResourceType resourceType = ResourceType.ExplanationOfBenefit;

        workJob(true, resourceType);
        Assertions.assertThrows(DataRetrievalException.class, () -> {
            dataService.retrieveData(orgID, providerID, Collections.singletonList(patientID.toString()), null, OffsetDateTime.now(ZoneOffset.UTC), resourceType);
        });
    }

    @Test
    public void whenGetJobBatchesReturnsCompletedJobWithResourceType() throws IllegalAccessException {
        UUID orgID = UUID.randomUUID();
        UUID providerID = UUID.randomUUID();
        UUID patientID = UUID.randomUUID();
        ResourceType resourceType = ResourceType.ExplanationOfBenefit;

        workJob(false, resourceType);
        Resource resource = dataService.retrieveData(orgID, providerID, Collections.singletonList(patientID.toString()), null, OffsetDateTime.now(ZoneOffset.UTC), resourceType);
        Assertions.assertTrue(resource instanceof Bundle);
    }

    @Test
    public void whenGetJobBatchesReturnsCompletedJobWithOperationOutcome() throws IllegalAccessException {
        UUID orgID = UUID.randomUUID();
        UUID providerID = UUID.randomUUID();
        UUID patientID = UUID.randomUUID();
        ResourceType resourceType = ResourceType.ExplanationOfBenefit;

        workJob(false, ResourceType.OperationOutcome);
        Resource resource = dataService.retrieveData(orgID, providerID, Collections.singletonList(patientID.toString()), null, OffsetDateTime.now(ZoneOffset.UTC), resourceType);
        Assertions.assertTrue(resource instanceof OperationOutcome);
    }

    private void workJob(boolean failBatch, ResourceType resourceType) throws IllegalAccessException {
        Mockito.doAnswer((mock) -> {
            Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
            while (workBatch.get().fetchNextPatient(aggregatorID).isPresent()) {
                queue.completePartialBatch(workBatch.get(), aggregatorID);
            }
            if (failBatch) {
                queue.failBatch(workBatch.get(), aggregatorID);
            } else {
                tmpFile = Files.newFile(String.format("%s/%s-%s.%s.ndjson", exportPath, workBatch.get().getBatchID().toString(), 0, resourceType.getPath()));
                workBatch.get().addJobQueueFile(resourceType, 0, 1);
                queue.completeBatch(workBatch.get(), aggregatorID);
            }
            return Collections.singletonList(workBatch.get());
        }).when(queue).getJobBatches(Mockito.any());

    }

}
