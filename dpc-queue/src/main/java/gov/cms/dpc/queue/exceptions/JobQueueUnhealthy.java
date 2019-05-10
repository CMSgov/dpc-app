package gov.cms.dpc.queue.exceptions;

public class JobQueueUnhealthy extends RuntimeException {

    public static final long serialVersionUID = 42L;

    public JobQueueUnhealthy(String message) {
        super(message);
    }

    public JobQueueUnhealthy(String message, Throwable exn) {
        super(message, exn);
    }
}
