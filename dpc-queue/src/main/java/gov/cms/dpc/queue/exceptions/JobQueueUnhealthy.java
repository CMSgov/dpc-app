package gov.cms.dpc.queue.exceptions;

import java.io.Serial;

public class JobQueueUnhealthy extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 42L;

    public JobQueueUnhealthy(String message) {
        super(message);
    }

    public JobQueueUnhealthy(String message, Throwable exn) {
        super(message, exn);
    }
}
