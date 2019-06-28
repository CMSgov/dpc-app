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
    void testStatus() {
        // Run through the states of a job
        final var jobID = UUID.randomUUID();
        final var job = new JobModel(jobID, List.of(ResourceType.Patient), "1", List.of("1"));
        assertAll(() -> assertEquals(JobStatus.QUEUED, job.getStatus()),
                () -> assertTrue(job.getSubmitTime().isPresent()),
                () -> assertTrue(job.getStartTime().isEmpty()));

        job.setRunningStatus();
        assertAll(() -> assertEquals(JobStatus.RUNNING, job.getStatus()),
                () -> assertTrue(job.getStartTime().isPresent()),
                () -> assertTrue(job.getCompleteTime().isEmpty()));

        job.setFinishedStatus(JobStatus.COMPLETED, List.of(new JobResult(jobID, ResourceType.Patient, 0, 1)));
        assertAll(() -> assertEquals(JobStatus.COMPLETED, job.getStatus()),
                () -> assertTrue(job.getCompleteTime().isPresent()));
    }
}
