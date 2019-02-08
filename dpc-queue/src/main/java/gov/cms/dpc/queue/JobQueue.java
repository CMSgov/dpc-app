package gov.cms.dpc.queue;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for submitting/retrieving export jobs to a backing queue.
 *
 */
public interface JobQueue {

    void submitJob(UUID jobID);

    Optional<JobStatus> getJobStatus(UUID jobID);

    Optional<UUID> workJob();


    void completeJob(UUID jobID, JobStatus status);
}


