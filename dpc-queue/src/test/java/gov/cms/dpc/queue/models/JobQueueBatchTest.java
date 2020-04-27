package gov.cms.dpc.queue.models;

import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class JobQueueBatchTest {

    private static final UUID jobID = UUID.randomUUID();
    private static final UUID orgID = UUID.randomUUID();
    private static final String providerID = "providerID";
    private static final List<ResourceType> resourceTypes = JobQueueBatch.validResourceTypes;
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
    void testFetchNextBatch() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.setRunningStatus(aggregatorID);
        Mockito.reset(job);

        final Optional<String> firstResult = job.fetchNextPatient(aggregatorID);
        assertEquals("1", firstResult.get());
        assertEquals(0, job.getPatientIndex().get());

        final Optional<String> secondResult = job.fetchNextPatient(aggregatorID);
        assertEquals("2", secondResult.get());
        assertEquals(1, job.getPatientIndex().get());

        final Optional<String> thirdResult = job.fetchNextPatient(aggregatorID);
        assertEquals("3", thirdResult.get());
        assertEquals(2, job.getPatientIndex().get());

        final Optional<String> done = job.fetchNextPatient(aggregatorID);
        assertTrue(done.isEmpty());
        assertEquals(2, job.getPatientIndex().get());

        Mockito.verify(job, Mockito.times(job.getPatients().size() + 1)).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testFetchNextBatch_NotRunning() {
        final var job = Mockito.spy(createJobQueueBatch());

        try {
            job.fetchNextPatient(aggregatorID);
            Assertions.fail();
        } catch ( JobQueueFailure e ) {
            assertTrue(e.getMessage().contains("Cannot fetch next batch."));
        }
    }

    @Test
    void testSetPausedStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.setRunningStatus(aggregatorID);
        Mockito.reset(job);

        job.setPausedStatus(aggregatorID);
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertTrue(job.getAggregatorID().isEmpty());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetPausedStatus_NotRunning() {
        final var job = Mockito.spy(createJobQueueBatch());

        try {
            job.setPausedStatus(aggregatorID);
            Assertions.fail();
        } catch ( JobQueueFailure e ) {
            assertTrue(e.getMessage().contains("Cannot pause batch."));
        }
    }

    @Test
    void testSetCompletedStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;
        job.patientIndex = 2;

        job.setCompletedStatus(aggregatorID);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertNull(job.patientIndex);

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetCompletedStatus_EmptyPatientList() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;
        job.patientIndex = null;
        job.patients = Collections.emptyList();

        job.setCompletedStatus(aggregatorID);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertNull(job.patientIndex);

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetFinishedStatus_InvalidRunningStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.QUEUED;

        try {
            job.setCompletedStatus(aggregatorID);
            Assertions.fail();
        } catch (JobQueueFailure e) {
            assertTrue(e.getMessage().contains("Cannot complete. JobStatus"));
        }
    }

    @Test
    void testSetFinishedStatus_NotDoneProcessing() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;

        try {
            job.setCompletedStatus(aggregatorID);
            Assertions.fail();
        } catch (JobQueueFailure e) {
            assertTrue(e.getMessage().contains("Cannot complete. Job processing not finished"));
        }
    }

    @Test
    void testSetFailedStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;
        job.patientIndex = 2;
        job.getJobQueueBatchFiles().add(new JobQueueBatchFile());

        job.setFailedStatus(aggregatorID);

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertEquals(2, job.patientIndex);
        assertTrue(job.getJobQueueBatchFiles().isEmpty());

        Mockito.verify(job, Mockito.never()).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetFailedStatus_InvalidRunningStatus() {
        // We should always allow a job to fail regardless of state, or this can cause other issues

        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.QUEUED;

        job.setFailedStatus(aggregatorID);

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertTrue(job.getJobQueueBatchFiles().isEmpty());

        Mockito.verify(job, Mockito.never()).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testRestartBatch() {
        final var job = createJobQueueBatch();
        job.setRunningStatus(aggregatorID);
        job.setFailedStatus(aggregatorID);

        job.restartBatch();

        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertNull(job.patientIndex);
        assertNull(job.startTime);
        assertNull(job.completeTime);
        assertNull(job.aggregatorID);
        assertTrue(job.getJobQueueBatchFiles().isEmpty());
    }

    @Test
    void testRestartBatch_Stuck() {
        final var job = createJobQueueBatch();
        job.setRunningStatus(aggregatorID);
        job.patientIndex = 2;
        job.getJobQueueBatchFiles().add(new JobQueueBatchFile());

        job.restartBatch();

        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertNull(job.patientIndex);
        assertNull(job.startTime);
        assertNull(job.completeTime);
        assertNull(job.aggregatorID);
        assertTrue(job.getJobQueueBatchFiles().isEmpty());
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

        Optional<String> result;
        do {
            result = job.fetchNextPatient(aggregatorID);
        } while (result.isPresent());

        job.setCompletedStatus(aggregatorID);
        assertAll(
                () -> assertEquals(JobStatus.COMPLETED, job.getStatus()),
                () -> assertTrue(job.getCompleteTime().isPresent()),
                () -> assertFalse(job.getAggregatorID().isPresent())
        );
    }

    JobQueueBatch createJobQueueBatch() {
        return new JobQueueBatch(jobID, orgID, providerID, patientList, resourceTypes, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

}
