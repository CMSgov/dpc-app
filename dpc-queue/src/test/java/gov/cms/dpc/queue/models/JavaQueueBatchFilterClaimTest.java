package gov.cms.dpc.queue.models;

import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class JobQueueBatchFilterClaimTest {

    @Test
    void testGetters() {
        final var batchID = UUID.randomUUID();
        final var jobID = UUID.randomUUID();
        final var claim = new JobQueueBatchFilterClaim(batchID, jobID, DPCResourceType.Patient, "status", "eq", "active");

        assertNotNull(claim.getId());
        assertEquals(batchID, claim.getBatchID());
        assertEquals(jobID, claim.getJobID());
        assertEquals(DPCResourceType.Patient, claim.getResourceType());
        assertEquals("status", claim.getFilterKey());
        assertEquals("eq", claim.getOperator());
        assertEquals("active", claim.getFilterValue());
    }

    @Test
    void testEquals() {
        final var batchID = UUID.randomUUID();
        final var jobID = UUID.randomUUID();
        final var a = new JobQueueBatchFilterClaim(batchID, jobID, DPCResourceType.Patient, "status", "eq", "active");
        final var b = new JobQueueBatchFilterClaim(batchID, jobID, DPCResourceType.Patient, "status", "eq", "active");
        final var c = new JobQueueBatchFilterClaim(UUID.randomUUID(), jobID, DPCResourceType.Patient, "status", "eq", "active");

        assertEquals(a, a);
        assertNotEquals(a, b, "different ids, should not be equal");
        assertNotEquals(a, c);
    }

    @Test
    void testEmptyConstructor() {
        // for hibernate
        final var claim = new JobQueueBatchFilterClaim();
        assertNull(claim.getId());
        assertNull(claim.getBatchID());
    }

    @Test
    void testToString() {
        final var batchID = UUID.randomUUID();
        final var jobID = UUID.randomUUID();
        final var claim = new JobQueueBatchFilterClaim(batchID, jobID, DPCResourceType.Patient, "status", "eq", "active");

        assertTrue(claim.toString().contains(batchID.toString()));
        assertTrue(claim.toString().contains("status"));
    }
}
