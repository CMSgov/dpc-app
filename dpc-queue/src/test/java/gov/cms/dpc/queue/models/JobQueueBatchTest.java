package gov.cms.dpc.queue.models;

import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Batch job queuing")
public class JobQueueBatchTest {

    private static final UUID jobID = UUID.randomUUID();
    private static final UUID orgID = UUID.randomUUID();
    private static final List<DPCResourceType> resourceTypes = JobQueueBatch.validResourceTypes;
    private static final UUID aggregatorID = UUID.randomUUID();
    private static final String orgNPI = NPIUtil.generateNPI();
    private static final String providerNPI = NPIUtil.generateNPI();
    private static final List<String> patientList = List.of("1", "2", "3");


    @Test
@DisplayName("Validate batch job resource type 🥳")
    void testIsValidResourceType() {
        assertTrue(JobQueueBatch.isValidResourceType(DPCResourceType.Patient));
        assertFalse(JobQueueBatch.isValidResourceType(DPCResourceType.Practitioner));
    }


    @Test
    @DisplayName("Verify batch job is valid 🥳")
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
@DisplayName("Verify batch job is v2 🥳")

    void testIsV2() {
        final var job = createJobQueueBatch();
        assertFalse(job.isV2());

        job.requestUrl = "/v2/";
        assertTrue(job.isV2());
    }

    @Test
@DisplayName("Create a job queue batch 🥳")

    void testCreateJobQueueBatch() {
        final var job = createJobQueueBatch();
        job.setAggregatorIDForTesting(aggregatorID);
        var completeTime = OffsetDateTime.now(ZoneOffset.UTC);
        job.setCompleteTime(completeTime);
        job.setPriority(1000);
        assertEquals(aggregatorID, job.getAggregatorID().get());
        assertEquals(completeTime, job.getCompleteTime().get());
        assertEquals(1000, job.getPriority());

        final var expected = "JobQueueBatch{" +
                             "batchID=" + job.getBatchID() +
                             ", jobID=" + job.getJobID() +
                             ", orgID=" + job.getOrgID() +
                             ", orgNPI='" + job.getOrgNPI() + '\'' +
                             ", providerID='" + job.getProviderID() + '\'' +
                             ", providerNPI='" + job.getProviderNPI() + '\'' +
                             ", status=" + job.getStatus() +
                             ", priority=" + 1000 +
                             ", patients=" + job.getPatients() +
                             ", patientIndex=" + (job.getPatientIndex().isPresent() ? job.getPatientIndex() : null) +
                             ", resourceTypes=" + job.getResourceTypes() +
                             ", since=" + (job.getSince().isPresent() ? job.getSince() : null) +
                             ", transactionTime=" + job.getTransactionTime() +
                             ", aggregatorID=" + aggregatorID +
                             ", updateTime=" + (job.getUpdateTime().isPresent() ? job.getUpdateTime() : null) +
                             ", submitTime=" + job.getSubmitTime().get() +
                             ", startTime=" + (job.getStartTime().isPresent() ? job.getStartTime() : null) +
                             ", completeTime=" + completeTime +
                             ", requestUrl='" + job.getRequestUrl() + '\'' +
                             ", requestingIP='" + job.getRequestingIP() + '\'' +
                             ", isBulk=" + job.isBulk() +
                             '}';
        assertEquals(expected, job.toString());
    }

    @Test
@DisplayName("Create a Job Queue Batch with two equal files 🥳")

    void testGetJobQueueBatchFile() {
        final var job = createJobQueueBatch();
        var file1 = job.addJobQueueFile(DPCResourceType.Patient, 0, 1);
        assertEquals(file1, job.getJobQueueFile(DPCResourceType.Patient).get());

        var file2 = job.addJobQueueFile(DPCResourceType.Patient, 0, 1);
        assertEquals(file1, file2);
        assertEquals(file1.getCount(), 2);
        assertEquals(file1, job.getJobQueueFileLatest(DPCResourceType.Patient).get());
    }

    @Test
@DisplayName("Set a batch job to running status 🥳")

    void testSetRunningStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.setRunningStatus(aggregatorID);

        assertEquals(JobStatus.RUNNING, job.getStatus());
        assertEquals(aggregatorID, job.getAggregatorID().get());
        assertNotNull(job.getStartTime());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Set unqueued job to running status 🤮")

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
@DisplayName("Get next batch from queue until empty 🥳")

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
        assertEquals(3, job.getPatientsProcessed());

        Mockito.verify(job, Mockito.times(job.getPatients().size() + 1)).verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Get next patient from non-running job 🤮")

    void testFetchNextBatch_NotRunning() {
        final var job = Mockito.spy(createJobQueueBatch());

        try {
            job.fetchNextPatient(aggregatorID);
            Assertions.fail();
        } catch (JobQueueFailure e) {
            assertTrue(e.getMessage().contains("Cannot fetch next batch."));
        }
    }

    @Test
@DisplayName("Pause a running job 🥳")

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
@DisplayName("Pause a non-running job 🤮")

    void testSetPausedStatus_NotRunning() {
        final var job = Mockito.spy(createJobQueueBatch());

        try {
            job.setPausedStatus(aggregatorID);
            Assertions.fail();
        } catch (JobQueueFailure e) {
            assertTrue(e.getMessage().contains("Cannot pause batch."));
        }
    }

    @Test
@DisplayName("Mark a running job with patients as complete 🥳")

    void testSetCompletedStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;
        job.patientIndex = 2;

        job.setCompletedStatus(aggregatorID);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertNull(job.patientIndex);
        assertEquals(3, job.getPatientsProcessed());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Mark a running job with empty patient list as complete 🥳")

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
        assertEquals(0, job.getPatientsProcessed());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Set a non-running job as complete 🤮")

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
@DisplayName("Set a running incomplete job as complete 🤮")

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
@DisplayName("Mark a running job as failed and verify partial results 🥳")

    void testSetFailedStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;
        job.patientIndex = 2;
        job.getJobQueueBatchFiles().add(new JobQueueBatchFile());

        job.setFailedStatus();

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertEquals(2, job.patientIndex);
        assertTrue(job.getJobQueueBatchFiles().isEmpty());
        assertEquals(3, job.getPatientsProcessed());

        Mockito.verify(job, Mockito.never()).verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Set a non-running job as failed 🤮")

    void testSetFailedStatus_InvalidRunningStatus() {
        // We should always allow a job to fail regardless of state, or this can cause other issues

        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.QUEUED;

        job.setFailedStatus();

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertFalse(job.getAggregatorID().isPresent());
        assertNotNull(job.getCompleteTime());
        assertTrue(job.getJobQueueBatchFiles().isEmpty());

        Mockito.verify(job, Mockito.never()).verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Restart a failed job 🥳")

    void testRestartBatch() {
        final var job = createJobQueueBatch();
        job.setRunningStatus(aggregatorID);
        job.setFailedStatus();

        job.restartBatch();

        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertNull(job.patientIndex);
        assertNull(job.startTime);
        assertNull(job.completeTime);
        assertNull(job.aggregatorID);
        assertTrue(job.getJobQueueBatchFiles().isEmpty());
        assertEquals(0, job.getPatientsProcessed());
    }

    @Test
@DisplayName("Restart a stuck job 🥳")

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
        assertEquals(0, job.getPatientsProcessed());
    }

    @Test
@DisplayName("Verify an unqueued job has no aggregator ID 🥳")

    void testVerifyAggregatorID_NoneSet() {
        final var job = createJobQueueBatch();
        job.verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Verify a queued job has an aggregator ID 🥳")

    void testVerifyAggregatorID_Match() {
        final var job = createJobQueueBatch();
        job.aggregatorID = aggregatorID;
        job.verifyAggregatorID(aggregatorID);
    }

    @Test
@DisplayName("Verify a claimed job can't be claimed by another aggregator 🤮")

    void testVerifyAggregatorID_InvalidMatch() {
        final var job = createJobQueueBatch();
        job.aggregatorID = UUID.randomUUID();

        try {
            job.verifyAggregatorID(aggregatorID);
            Assertions.fail();
        } catch (JobQueueFailure e) {
            assertTrue(e.getMessage().contains("Cannot process a job owned by another aggregator."));
        }
    }

    @Test
@DisplayName("Verify complete job queue batch lifecycle 🥳")

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
        return new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, OffsetDateTime.now(ZoneOffset.UTC), null, null,true);
    }


    @Test
@DisplayName("Verify overriden equals on Job Queue Batch with dates 🥳")

    void testEquals() {
        OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

        try (MockedStatic<UUID> uuidMockedStatic = Mockito.mockStatic(UUID.class)) {
            uuidMockedStatic.when(UUID::randomUUID).thenReturn(jobID);

            var job1 = new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, timestamp, null, null,true);
            var job2 = new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, timestamp, null, null,true);
            job1.submitTime = timestamp;
            job2.submitTime = timestamp;
            assertEquals(job1, job2);
        }
    }

}
