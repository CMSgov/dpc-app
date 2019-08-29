package gov.cms.dpc.queue.models;

import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JobQueueBatchTest {

    private static final UUID jobID = UUID.randomUUID();
    private static final UUID batchID = UUID.randomUUID();
    private static final UUID orgID = UUID.randomUUID();
    private static final UUID aggregatorID = UUID.randomUUID();
    private static final List<String> patientList = List.of("1", "2", "3");

    @Test
    void testIsValid() {
        final var job = createJobQueueBatch();
        assertTrue(job.isValid());

        job.aggregatorID = aggregatorID;
        assertFalse(job.isValid());

        job.status = JobStatus.RUNNING;
        assertFalse(job.isValid());

        job.startTime = OffsetDateTime.now(ZoneOffset.UTC);
        job.updateTime = OffsetDateTime.now(ZoneOffset.UTC);
        job.submitTime = OffsetDateTime.now(ZoneOffset.UTC);
        assertTrue(job.isValid());

        job.completeTime = OffsetDateTime.now(ZoneOffset.UTC);
        job.status = JobStatus.COMPLETED;
        assertFalse(job.isValid());

        job.aggregatorID = null;
        assertTrue(job.isValid());
    }

    @Test
    void testSetRunningStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.setRunningStatus(aggregatorID);

        assertEquals(JobStatus.RUNNING, job.getStatus());
        assertEquals(aggregatorID, job.getAggregatorID().get());
        assertNotNull(job.getStartTime());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetRunningStatus_NotInQueuedState() {
        final var job = createJobQueueBatch();
        job.status = JobStatus.RUNNING;

        try {
            job.setRunningStatus(aggregatorID);
            Assertions.fail();
        } catch (JobQueueFailure e) {
            assertTrue(e.getMessage().contains("Cannot run job. JobStatus"));
        }
    }

    @Test
    void testSetFinishedStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;

        job.setFinishedStatus(aggregatorID, JobStatus.COMPLETED);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetFinishedStatus_InvalidCompletionStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;

        try {
            job.setFinishedStatus(aggregatorID, JobStatus.QUEUED);
            Assertions.fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    void testSetFinishedStatus_InvalidRunningStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.QUEUED;

        try {
            job.setFinishedStatus(aggregatorID, JobStatus.FAILED);
            Assertions.fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Cannot complete."));
        }
    }

    @Test
    void testVerifyAggregatorID_NoneSet() throws Exception {
        final var job = createJobQueueBatch();
        job.verifyAggregatorID(aggregatorID);
    }

    @Test
    void testVerifyAggregatorID_Match() throws Exception {
        final var job = createJobQueueBatch();
        job.aggregatorID = aggregatorID;
        job.verifyAggregatorID(aggregatorID);
    }

    @Test
    void testVerifyAggregatorID_InvalidMatch() throws Exception {
        final var job = createJobQueueBatch();
        job.aggregatorID = UUID.randomUUID();

        try {
            job.verifyAggregatorID(aggregatorID);
            Assertions.fail();
        } catch ( JobQueueFailure e ) {
            assertTrue(e.getMessage().contains("Cannot process a job owned by another aggregator."));
        }
    }

    @Test
    void testStatus_OverallFlow() {
        final var job = createJobQueueBatch();

        assertAll(() -> assertEquals(JobStatus.QUEUED, job.getStatus()),
                () -> assertTrue(job.getSubmitTime().isPresent()),
                () -> assertTrue(job.getStartTime().isEmpty()));

        job.setRunningStatus(aggregatorID);
        assertAll(
                () -> assertEquals(JobStatus.RUNNING, job.getStatus()),
                () -> assertTrue(job.getStartTime().isPresent()),
                () -> assertTrue(job.getCompleteTime().isEmpty()),
                () -> assertTrue(job.getAggregatorID().isPresent())
        );

        job.setFinishedStatus(aggregatorID, JobStatus.COMPLETED);
        assertAll(
                () -> assertEquals(JobStatus.COMPLETED, job.getStatus()),
                () -> assertTrue(job.getCompleteTime().isPresent()),
                () -> assertFalse(job.getAggregatorID().isPresent())
        );
    }

    JobQueueBatch createJobQueueBatch() {
        return new JobQueueBatch(jobID, batchID, orgID, patientList);
    }

}
