package gov.cms.dpc.queue;

import gov.cms.dpc.queue.models.JobQueueBatch;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for submitting/retrieving export jobs to a backing queue.
 */
public interface JobQueueInterface {

    /**
     * Create and submit a job into the queue. The job will be broken into batches, prioritized,
     * and set to the QUEUED status.
     *
     * @param orgID - The organization submitting the job
     * @param providerID - The provider submitting the job
     * @param patients - The list of patients to fetch data for
     * @param resourceTypes - The resource types to fetch patient data for
     * @return The UUID of the created job
     */
    UUID createJob(UUID orgID, String providerID, List<String> patients, List<ResourceType> resourceTypes);

    /**
     * Find a batch in the queue, regardless of job status. Does not alter the batch.
     *
     * @param batchID - the id of the batch to search
     * @return Optional that contains the found job if present
     */
    Optional<JobQueueBatch> getBatch(UUID batchID);

    /**
     * Find all the batches of a given job, regardless of job status. Does not alter the job.
     *
     * @param jobID - the id of the job to search
     * @return a list of batches part of the job if present
     */
    List<JobQueueBatch> getJobBatches(UUID jobID);

    /**
     * Find the next job that is ready to run. Alter the job's {@link JobStatus} to `RUNNING`.
     *
     * @return The job to work, if present.
     */
    Optional<JobQueueBatch> workBatch(UUID aggregatorID);

    /**
     * Pauses the current progress and allows another aggregator to pick up the batch.
     *
     * @param job - the job to pause
     * @param aggregatorID - the current aggregator working the job
     */
    void pauseBatch(JobQueueBatch job, UUID aggregatorID);

    /**
     * Alter the job's {@link JobStatus} to passed status. Called when the job batch is finished partially processing.
     *
     * @param job - the job to add progress to
     * @param aggregatorID - the current aggregator working the job
     */
    void completePartialBatch(JobQueueBatch job, UUID aggregatorID);

    /**
     * Completes the current batch.
     *
     * @param job - the job batch to compelte
     * @param aggregatorID - the current aggregator working the job
     */
    void completeBatch(JobQueueBatch job, UUID aggregatorID);

    /**
     * Fails the current batch. A failed batch will have to be manually restarted.
     *
     * @param job - the job batch to fail
     * @param aggregatorID - the current aggregator working the job
     */
    void failBatch(JobQueueBatch job, UUID aggregatorID);

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


