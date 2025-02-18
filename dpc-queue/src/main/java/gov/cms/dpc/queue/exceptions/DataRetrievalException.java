package gov.cms.dpc.queue.exceptions;

import java.io.Serial;

public class DataRetrievalException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8577472916601690559L;

    public DataRetrievalException(String message) {
        super(message);
    }

    public DataRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
