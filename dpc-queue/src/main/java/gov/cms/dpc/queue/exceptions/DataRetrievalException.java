package gov.cms.dpc.queue.exceptions;

public class DataRetrievalException extends RuntimeException {

    private static final long serialVersionUID = 8577472916601690559L;

    public DataRetrievalException(String message) {
        super(message);
    }

    public DataRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}