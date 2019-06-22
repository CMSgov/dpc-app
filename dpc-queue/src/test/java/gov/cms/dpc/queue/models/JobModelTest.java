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
        final var c = new JobModel(jobID, List.of(ResourceType.ExplanationOfBenefit), "1", List.of("1"));
        assertFalse(a.equals(c), "expected a to not equal c");
        assertTrue(a.equals(a));
    }

    @Test
    void testHash() {
        final var jobID = UUID.randomUUID();
        final var a = new JobModel(jobID, List.of(ResourceType.Patient), "1", List.of("1"));
        final var c = new JobModel(jobID, List.of(ResourceType.ExplanationOfBenefit), "1", List.of("1"));
        assertNotEquals(a.hashCode(), c.hashCode(), "expected a to not equal c");
    }

    @Test
    void testCompetedHash() {
        final var jobID = UUID.randomUUID();
        final var a = new JobModel(jobID, List.of(ResourceType.Patient), "1", List.of("1"));
        final var completedA = a.makeRunningJob().makeFinishedJob(JobStatus.COMPLETED, List.of(new JobResult(jobID, ResourceType.Patient, 0, 1)));;
        assertNotEquals(completedA.hashCode(), a.hashCode(), "expected completed to not equal not-completed");
    }
}
