package gov.cms.dpc.queue.exceptions;

import java.util.UUID;

public class JobQueueFailure extends RuntimeException {

    public static final long serialVersionUID = 42L;

    public JobQueueFailure(String message) {
        super(message);
    }

    public JobQueueFailure(UUID jobID, UUID batchID, String message) {
        super(String.format("Operation on Job(%s) Batch(%s) failed for reason: %s", jobID, batchID, message));
    }

    public JobQueueFailure(UUID jobID, UUID batchID, Throwable t) {
        super(String.format("Operation on Job(%s) Batch(%s) failed.", jobID, batchID), t);
    }

    public JobQueueFailure(UUID jobID, UUID batchID, String message, Throwable throwable) {
        super(String.format("Operation on Job(%s) Batch(%s) failed for reason: %s", jobID, batchID, message), throwable);
    }
}
