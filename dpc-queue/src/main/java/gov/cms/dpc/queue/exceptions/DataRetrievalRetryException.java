package gov.cms.dpc.queue.exceptions;

import java.io.Serial;

public class DataRetrievalRetryException extends Exception {

    @Serial
    private static final long serialVersionUID = 6084354194337543376L;

    public DataRetrievalRetryException(String message) {
        super(message);
    }

    public DataRetrievalRetryException() {
        super();
    }
}
