package gov.cms.dpc.queue;

import gov.cms.dpc.queue.models.JobResult;
import gov.cms.dpc.queue.models.JobModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for submitting/retrieving export jobs to a backing queue.
 */
// TODO (rickhawes) - Relook at this interface in the case of DPC-205 where we need to store results from a job
public interface JobQueue {
    /**
     * Submit a job into the queue. The job's {@link JobStatus} will be set to `QUEUED`, meaning
     * that the job is ready to run.
     *
     * @param jobID - the jobID of the new job
     * @param job - the job model that describes the job
     */
    void submitJob(UUID jobID, JobModel job);

    /**
     * Find a job in the queue, regardless of job status. Does not alter the job.
     *
     * @param jobID - the id of the job to search
     * @return Optional that contains the found job if present
     */
    Optional<JobModel> getJob(UUID jobID);

    /**
     * Find the next job that is ready to run. Alter the job's {@link JobStatus} to `RUNNING`.
     *
     * @return The jobid/job to work, if present.
     */
    Optional<Pair<UUID, JobModel>> workJob();

    /**
     * Alter the job's {@link JobStatus} to passed status. Called when the job's is finished.
     *
     * @param jobID - the job
     * @param status - the new  {@link JobStatus} of the job. Must be `COMPLETED` or `FAILED`.
     * @param results - The new counts for each job resource type.
     */
    void completeJob(UUID jobID, JobStatus status, List<JobResult> results);

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


