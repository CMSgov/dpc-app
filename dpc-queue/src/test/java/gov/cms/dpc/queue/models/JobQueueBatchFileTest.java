package gov.cms.dpc.queue.models;

import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class JobQueueBatchFileTest {
    @Test
    void testEqual() {
        final var jobID = UUID.randomUUID();
        final var batchID = UUID.randomUUID();
        final var a = new JobQueueBatchFile(jobID, batchID, DPCResourceType.Patient, 0, 1);
        final var b = new JobQueueBatchFile(jobID, batchID, DPCResourceType.Patient, 0, 1);
        final var c = new JobQueueBatchFile(jobID, batchID, DPCResourceType.ExplanationOfBenefit, 0, 1);
        assertTrue(a.equals(b), "expected a to equal b");
        assertFalse(a.equals(c), "expected a to not equal c");
        assertTrue(a.equals(a));
    }

    @Test
    void testHash() {
        final var jobID = UUID.randomUUID();
        final var batchID = UUID.randomUUID();
        final var a = new JobQueueBatchFile(jobID, batchID, DPCResourceType.Patient, 0, 1);
        final var b = new JobQueueBatchFile(jobID, batchID, DPCResourceType.Patient, 0, 1);
        final var c = new JobQueueBatchFile(jobID, batchID, DPCResourceType.ExplanationOfBenefit, 0, 1);
        assertEquals(a.hashCode(), b.hashCode(), "expected a to equal b");
        assertNotEquals(a.hashCode(), c.hashCode(), "expected a to not equal c");
    }

    @Test
    void testGet() {
        final var jobID = UUID.randomUUID();
        final var batchID = UUID.randomUUID();
        final var a = new JobQueueBatchFile(jobID, batchID, DPCResourceType.Patient, 0, 1);
        assertEquals(jobID, a.getJobID());
        assertEquals(DPCResourceType.Patient, a.getResourceType());
        assertEquals(0, a.getSequence());
        assertEquals(1, a.getCount());
        assertEquals(new JobQueueBatchFile.JobQueueBatchFileID(batchID, DPCResourceType.Patient, 0), a.getJobQueueBatchFileID());;
    }

    @Test
    void testParsesFileName() {
        final UUID batchID = UUID.randomUUID();
        final int seq = 1;
        final DPCResourceType resourceType = DPCResourceType.Patient;
        String fileName = JobQueueBatchFile.formOutputFileName(batchID, resourceType, seq);

        JobQueueBatchFile.JobQueueBatchFileID fileId = JobQueueBatchFile.getFileIdFromName(fileName);
        assertEquals(batchID, fileId.getBatchID());
        assertEquals(seq, fileId.getSequence());
        assertEquals(DPCResourceType.Patient, fileId.getResourceType());
    }

    @Test
    void testParsesFileNameWithLargeSequence() {
        final UUID batchID = UUID.randomUUID();
        final int seq = 10;
        final DPCResourceType resourceType = DPCResourceType.Patient;
        String fileName = JobQueueBatchFile.formOutputFileName(batchID, resourceType, seq);

        JobQueueBatchFile.JobQueueBatchFileID fileId = JobQueueBatchFile.getFileIdFromName(fileName);
        assertEquals(batchID, fileId.getBatchID());
        assertEquals(seq, fileId.getSequence());
        assertEquals(DPCResourceType.Patient, fileId.getResourceType());
    }

    @Test
    void testHandlesBadFileName() {
        final String fileName = "bad_file_name";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JobQueueBatchFile.getFileIdFromName(fileName));
        assertEquals(String.format("Could not parse file name: %s", fileName), e.getMessage());
    }

    @Test
    void testHandlesBadUUIDInFileName() {
        final String fileName = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx-0.explanationofbenefit";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JobQueueBatchFile.getFileIdFromName(fileName));
        assertEquals(String.format("Could not parse file name: %s", fileName), e.getMessage());
    }

    @Test
    void testHandlesBadSeqInFileName() {
        final UUID batchId = UUID.randomUUID();
        final String fileName = String.format("%s-X.explanationofbenefit", batchId);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JobQueueBatchFile.getFileIdFromName(fileName));
        assertEquals(String.format("Could not parse file name: %s", fileName), e.getMessage());
    }

    @Test
    void testHandlesBadResourceInFileName() {
        final UUID batchId = UUID.randomUUID();
        final String fileName = String.format("%s-0.fake_resource", batchId);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JobQueueBatchFile.getFileIdFromName(fileName));
        assertEquals(String.format("Could not parse file name: %s", fileName), e.getMessage());
    }

    @Test
    void testHandlesExtraCharsInFileName() {
        final UUID batchId = UUID.randomUUID();
        final String fileName = String.format("%s-0.%s.extraChars", batchId, DPCResourceType.Patient.getPath());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JobQueueBatchFile.getFileIdFromName(fileName));
        assertEquals(String.format("Could not parse file name: %s", fileName), e.getMessage());
    }
}
