package gov.cms.dpc.queue;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for submitting/retrieving export jobs to a backing queue.
 *
 */
public interface JobQueue {

    <T> void submitJob(UUID jobID, T data);

    Optional<JobStatus> getJobStatus(UUID jobID);

    <T> Optional<Pair<UUID, T>> workJob();

    void completeJob(UUID jobID, JobStatus status);

    void removeJob(UUID jobID);

    int queueSize();
}


