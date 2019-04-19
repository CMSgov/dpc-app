package gov.cms.dpc.queue;

import gov.cms.dpc.queue.models.JobModel;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for submitting/retrieving export jobs to a backing queue.
 */
public interface JobQueue {

    void submitJob(UUID jobID, JobModel data);

    Optional<JobStatus> getJobStatus(UUID jobID);

    Optional<JobModel> getJob(UUID jobID);

    Optional<Pair<UUID, JobModel>> workJob();

    void completeJob(UUID jobID, JobStatus status);

    /**
     * Number of items in the queue.
     * For the {@link MemoryQueue}, this returns the number of jobs with the {@link JobStatus#QUEUED}.
     *
     * @return - {@link long} number of jobs waiting to be run
     */
    long queueSize();

    /**
     * Returns the name of the type of underlying queue
     *
     * @return - {@link String} queue type
     */
    String queueType();
}


