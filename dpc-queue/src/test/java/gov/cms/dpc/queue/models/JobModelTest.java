package gov.cms.dpc.queue.models;

import gov.cms.dpc.queue.JobStatus;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

class JobModelTest {
    @Test
    void testEqual() {
        final var jobID = UUID.randomUUID();
        final var a = new JobModel(jobID, List.of(ResourceType.Patient), "1", List.of("1"));
        final var b = a.clone();
        final var c = new JobModel(jobID, List.of(ResourceType.ExplanationOfBenefit), "1", List.of("1"));
        assertTrue(a.equals(b), "expected a to equal b");
        assertFalse(a.equals(c), "expected a to not equal c");
        assertTrue(a.equals(a));
    }

    @Test
    void testHash() {
        final var jobID = UUID.randomUUID();
        final var a = new JobModel(jobID, List.of(ResourceType.Patient), "1", List.of("1"));
        final var b = a.clone();
        final var c = new JobModel(jobID, List.of(ResourceType.ExplanationOfBenefit), "1", List.of("1"));
        assertEquals(a.hashCode(), b.hashCode(), "expected a to equal b");
        assertNotEquals(a.hashCode(), c.hashCode(), "expected a to not equal c");
    }

    @Test
    void testCompetedHash() {
        final var jobID = UUID.randomUUID();
        final var a = new JobModel(jobID, List.of(ResourceType.Patient), "1", List.of("1"));
        final var b = a.clone();
        final var completedA = a.makeRunningJob().makeFinishedJob(JobStatus.COMPLETED, List.of(new JobResult(jobID, ResourceType.Patient, 0, 1)));
        final var completedB = completedA.clone();
        assertEquals(a.hashCode(), b.hashCode(), "expected a to equal b");
        assertEquals(completedA.hashCode(), completedB.hashCode(), "expected a to equal b");
    }
}
