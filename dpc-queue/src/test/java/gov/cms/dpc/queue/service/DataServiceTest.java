package gov.cms.dpc.queue.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.gzip.GzipUtil;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.FileManager;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryBatchQueue;
import gov.cms.dpc.queue.exceptions.DataRetrievalException;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import org.apache.commons.io.FileUtils;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataServiceTest {
    @Spy
    private MemoryBatchQueue queue;

    @Spy
    private FhirContext fhirContext;

    private final UUID aggregatorID = UUID.randomUUID();
    private final UUID orgID = UUID.randomUUID();
    private final UUID patientID = UUID.randomUUID();
    private final String orgNPI = NPIUtil.generateNPI();
    private final String providerNPI = NPIUtil.generateNPI();
    private final String exportPath = "/tmp";
	private DataService dataService;
    private File tmpFile;

    @BeforeEach
    void before() {
        MockitoAnnotations.openMocks(this);
		FileManager fileManager = new FileManager(exportPath, queue);
        dataService = new DataService(queue, fhirContext, exportPath, 1, fileManager);
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
    void whenGetJobBatchesReturnsCompletedJobWithResourceTypeFromCompressedExport() {
        DPCResourceType resourceType = DPCResourceType.ExplanationOfBenefit;

        workJob(false, resourceType, true);
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
        workJob(failBatch, resourceType, false);
    }

    private void workJob(boolean failBatch, DPCResourceType resourceType, boolean compressedFile) {
        Mockito.doAnswer(mock -> {
            Optional<JobQueueBatch> workBatch = queue.claimBatch(aggregatorID);
            while (workBatch.flatMap(batch -> batch.fetchNextPatient(aggregatorID)).isPresent()) {
                queue.completePartialBatch(workBatch.get(), aggregatorID);
            }
            if (failBatch) {
                queue.failBatch(workBatch.get(), aggregatorID);
            } else {
                String fileFormat;
                byte[] tmpData;
                if (compressedFile) {
                    fileFormat = "%s/%s-%s.%s.ndjson.gz";
                    // If we don't write a GZip header to the file, we won't be able to open up the input stream
                    tmpData = GzipUtil.compress("");
                } else {
                    fileFormat = "%s/%s-%s.%s.ndjson";
                    tmpData = new byte[0];
                }

                tmpFile = Files.newFile(String.format(fileFormat, exportPath, workBatch.get().getBatchID().toString(), 0, resourceType.getPath()));
                FileUtils.writeByteArrayToFile(tmpFile, tmpData);
                JobQueueBatchFile batchFile = workBatch.get().addJobQueueFile(resourceType, 0, 1);
                batchFile.setChecksum(new byte[] {1, 2, 3});
                queue.completeBatch(workBatch.get(), aggregatorID);
            }
            return List.of(workBatch.get());
        }).when(queue).getJobBatches(Mockito.any());
    }

    @Test
    void createJobPutsJobOnQueue() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID jobId = dataService.createJob(
            orgID,
            orgNPI,
            providerNPI,
            List.of("mbi"),
            List.of(DPCResourceType.Patient),
            now,
            now,
            "ip",
            "url",
            false,
            false
        );

        List<JobQueueBatch> batches = queue.getJobBatches(jobId);
        JobQueueBatch batch = batches.get(0);

        assertEquals(1, batches.size());
        assertEquals(jobId, batch.getJobID());
        assertEquals(JobStatus.QUEUED, batch.getStatus());
    }
}
