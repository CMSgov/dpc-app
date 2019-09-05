package gov.cms.dpc.queue;

import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for submitting/retrieving export jobs to a backing queue.
 */
public interface JobQueueInterface {
    /**
     * Submit a job into the queue. The job's {@link JobStatus} will be set to `QUEUED`, meaning
     * that the job is ready to run.
     *
     * @param job - the job model that describes the job
     */
    void submitJob(JobQueueBatch job);

    /**
     * Find a job in the queue, regardless of job status. Does not alter the job.
     *
     * @param jobID - the id of the job to search
     * @param batchID = the id of the batch to search
     * @return Optional that contains the found job if present
     */
    Optional<JobQueueBatch> getJobBatch(UUID jobID, UUID batchID);

    /**
     * Find all the batches of a given job, regardless of job status. Does not alter the job.
     *
     * @param jobID
     * @return a list of batches part of the job if present
     */
    List<JobQueueBatch> getJobBatches(UUID jobID);

    /**
     * Find the next job that is ready to run. Alter the job's {@link JobStatus} to `RUNNING`.
     *
     * @return The job to work, if present.
     */
    Optional<JobQueueBatch> workJob();

    /**
     * Alter the job's {@link JobStatus} to passed status. Called when the job batch is finished.
     *
     * @param job - the job
     * @param results - The new counts for each job resource type.
     */
    void completeJob(JobQueueBatch job, List<JobQueueBatchFile> results);

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

    /**
     * Determines if the underlying queue is healthy or not.
     * This is accomplished by
     * throws if not healthy
     */
    void assertHealthy();
}


