package gov.cms.dpc.queue.models;

import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Batch job queuing with files")

class JobQueueBatchFileTest {
    @Test
@DisplayName("Verify overriden equals on Job Queue Batch File 🥳")

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
@DisplayName("Verify overriden hashcode on Job Queue Batch File 🥳")

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
@DisplayName("Get a Job Queue Batch File 🥳")

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
}
