package gov.cms.dpc.queue.exceptions;

public class JobQueueUnhealthy extends RuntimeException {

    public JobQueueUnhealthy(String message) {
        super(message);
    }

    public JobQueueUnhealthy(String message, Throwable exn) {
        super(message, exn);
    }
}
