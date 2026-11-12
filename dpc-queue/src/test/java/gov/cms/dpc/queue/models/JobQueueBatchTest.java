package gov.cms.dpc.queue.models;

import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.testing.BufferedLoggerHandler;
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

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
class JobQueueBatchTest {

    private static final UUID jobID = UUID.randomUUID();
    private static final UUID orgID = UUID.randomUUID();
    private static final List<DPCResourceType> resourceTypes = JobQueueBatch.validResourceTypes;
    private static final UUID aggregatorID = UUID.randomUUID();
    private static final UUID groupID = UUID.randomUUID();
    private static final UUID oneEcoJobID = UUID.randomUUID();
    private static final String orgNPI = NPIUtil.generateNPI();
    private static final String providerNPI = NPIUtil.generateNPI();
    private static final List<String> patientList = List.of("1", "2", "3");


    @Test
    void testIsValidResourceType() {
        assertTrue(JobQueueBatch.isValidResourceType(DPCResourceType.Patient));
        assertFalse(JobQueueBatch.isValidResourceType(DPCResourceType.Practitioner));
    }


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
    void testIsV2() {
        final var job = createJobQueueBatch();
        assertFalse(job.isV2());

        job.requestUrl = "/v2/";
        assertTrue(job.isV2());
    }

    @Test
    void testIsV3() {
        final var job = createV3JobQueueBatch();
        assertTrue(job.isV3());
    }

    @Test
    void testDefaultExportFields() {
        final var job = createJobQueueBatch();
        assertEquals("internal-dpc", job.getExportSource());
        assertEquals("V1", job.getExportVersion());
        assertTrue(job.getGroupID().isEmpty());
    }

    @Test
    void testV3ConstructorFields() {
        final var job = createV3JobQueueBatch();
        assertEquals(groupID, job.getGroupID().get());
        assertEquals("external-bcda", job.getExportSource());
        assertEquals("V3", job.getExportVersion());
    }

    @Test
    void testOneEcoJobID() {
        final var job = createJobQueueBatch();
        assertTrue(job.getOneEcoJobID().isEmpty());

        job.setOneEcoJobID(oneEcoJobID);
        assertEquals(oneEcoJobID, job.getOneEcoJobID().get());
    }

    @Test
    void testCreateJobQueueBatch() {
        final var job = createJobQueueBatch();
        job.setAggregatorIDForTesting(aggregatorID);
        var completeTime = OffsetDateTime.now(ZoneOffset.UTC);
        job.setCompleteTime(completeTime);
        job.setPriority(1000);
        job.setOneEcoJobID(oneEcoJobID);
        assertEquals(aggregatorID, job.getAggregatorID().get());
        assertEquals(completeTime, job.getCompleteTime().get());
        assertEquals(1000, job.getPriority());
        assertEquals(oneEcoJobID, job.getOneEcoJobID().get());

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
                ", groupID=" + null +
                ", exportSource='" + job.getExportSource() + '\'' +
                ", exportVersion='" + job.getExportVersion() + '\'' +
                ", oneEcoJobID=" + oneEcoJobID +
                '}';
        assertEquals(expected, job.toString());
    }

    @Test
    void testGetJobQueueBatchFile() {
        final var job = createJobQueueBatch();
        var file1 = job.addJobQueueFile(DPCResourceType.Patient, 0, 1);
        assertEquals(file1, job.getJobQueueFile(DPCResourceType.Patient).get());

        var file2 = job.addJobQueueFile(DPCResourceType.Patient, 0, 1);
        assertEquals(file1, file2);
        assertEquals(2, file1.getCount());
        assertEquals(file1, job.getJobQueueFileLatest(DPCResourceType.Patient).get());
    }

    @Test
    void testAddJobQueueBatchFilterClaim() {
        final var job = createV3JobQueueBatch();
        assertTrue(job.getJobQueueBatchFilterClaims().isEmpty());

        var claim = job.addJobQueueBatchFilterClaim(DPCResourceType.Patient, "status", "eq", "active");

        assertEquals(1, job.getJobQueueBatchFilterClaims().size());
        assertEquals(claim, job.getJobQueueBatchFilterClaims().getFirst());
        assertEquals(job.getBatchID(), claim.getBatchID());
        assertEquals(job.getJobID(), claim.getJobID());
        assertEquals(DPCResourceType.Patient, claim.getResourceType());
        assertEquals("status", claim.getFilterKey());
        assertEquals("eq", claim.getOperator());
        assertEquals("active", claim.getFilterValue());
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

        assertThrows(JobQueueFailure.class, () -> job.setRunningStatus(aggregatorID));
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
        assertEquals(3, job.getPatientsProcessed());

        Mockito.verify(job, Mockito.times(job.getPatients().size() + 1)).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testFetchNextBatch_NotRunning() {
        final var job = Mockito.spy(createJobQueueBatch());

        assertThrows(JobQueueFailure.class, () -> job.fetchNextPatient(aggregatorID));
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
        job.status = JobStatus.COMPLETED;

        assertThrows(JobQueueFailure.class, () -> job.setPausedStatus(aggregatorID));
    }

    @Test
    void testSetPausedStatus_AlreadyPaused() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.setPausedStatus(aggregatorID);

        assertDoesNotThrow(() -> {
            job.setPausedStatus(aggregatorID);
        });
        assertEquals(JobStatus.QUEUED, job.getStatus());
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
        assertEquals(3, job.getPatientsProcessed());

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
        assertEquals(0, job.getPatientsProcessed());

        Mockito.verify(job).verifyAggregatorID(aggregatorID);
    }

    @Test
    void testSetFinishedStatus_InvalidRunningStatus() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.QUEUED;

        assertThrows(JobQueueFailure.class, () -> job.setCompletedStatus(aggregatorID));
    }

    @Test
    void testSetFinishedStatus_NotDoneProcessing() {
        final var job = Mockito.spy(createJobQueueBatch());
        job.status = JobStatus.RUNNING;

        assertThrows(JobQueueFailure.class, () -> job.setCompletedStatus(aggregatorID));
    }

    @Test
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
    void testVerifyAggregatorID_NoneSet() {
        final var job = createJobQueueBatch();
        job.verifyAggregatorID(aggregatorID);
    }

    @Test
    void testVerifyAggregatorID_Match() {
        final var job = createJobQueueBatch();
        job.aggregatorID = aggregatorID;
        job.verifyAggregatorID(aggregatorID);
    }

    @Test
    void testVerifyAggregatorID_InvalidMatch() {
        final var job = createJobQueueBatch();
        job.aggregatorID = UUID.randomUUID();

        assertThrows(JobQueueFailure.class, () -> job.verifyAggregatorID(aggregatorID));
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
        return new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, OffsetDateTime.now(ZoneOffset.UTC), null, null, true);
    }

    JobQueueBatch createV3JobQueueBatch() {
        return new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, OffsetDateTime.now(ZoneOffset.UTC), null, null, true,
                groupID, "external-bcda", "V3");
    }


    @Test
    void testEquals() {
        OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

        try (MockedStatic<UUID> uuidMockedStatic = Mockito.mockStatic(UUID.class)) {
            uuidMockedStatic.when(UUID::randomUUID).thenReturn(jobID);

            var job1 = new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, timestamp, null, null, true);
            var job2 = new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, timestamp, null, null, true);
            job1.submitTime = timestamp;
            job2.submitTime = timestamp;
            assertEquals(job1, job2);
            assertEquals(job1.hashCode(), job2.hashCode());
        }
    }

    @Test
    void testEquals_differentExportVersion() {
        OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

        try (MockedStatic<UUID> uuidMockedStatic = Mockito.mockStatic(UUID.class)) {
            uuidMockedStatic.when(UUID::randomUUID).thenReturn(jobID);

            var job1 = new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, timestamp, null, null, true);
            var job2 = new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patientList, resourceTypes, null, timestamp, null, null, true,
                    groupID, "external-bcda", "V3");
            job1.submitTime = timestamp;
            job2.submitTime = timestamp;
            assertNotEquals(job1, job2);
        }
    }

}
