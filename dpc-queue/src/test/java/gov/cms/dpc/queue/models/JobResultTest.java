package gov.cms.dpc.queue.models;

import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

public class JobResultTest {
    @Test
    void testEqual() {
        final var jobID = UUID.randomUUID();
        final var a = new JobResult(jobID, ResourceType.Patient, 0, 1);
        final var b = new JobResult(jobID, ResourceType.Patient, 0, 1);
        final var c = new JobResult(jobID, ResourceType.ExplanationOfBenefit, 0, 1);
        assertTrue(a.equals(b), "expected a to equal b");
        assertFalse(a.equals(c), "expected a to not equal c");
        assertTrue(a.equals(a));
    }

    @Test
    void testHash() {
        final var jobID = UUID.randomUUID();
        final var a = new JobResult(jobID, ResourceType.Patient, 0, 1);
        final var b = new JobResult(jobID, ResourceType.Patient, 0, 1);
        final var c = new JobResult(jobID, ResourceType.ExplanationOfBenefit, 0, 1);
        assertEquals(a.hashCode(), b.hashCode(), "expected a to equal b");
        assertNotEquals(a.hashCode(), c.hashCode(), "expected a to not equal c");
    }

    @Test
    void testGet() {
        final var jobID = UUID.randomUUID();
        final var a = new JobResult(jobID, ResourceType.Patient, 0, 1);
        assertEquals(jobID, a.getJobID());
        assertEquals(ResourceType.Patient, a.getResourceType());
        assertEquals(0, a.getSequence());
        assertEquals(1, a.getCount());
        assertEquals(new JobResult.JobResultID(jobID, ResourceType.Patient, 0), a.getJobResultID());;
    }
}
