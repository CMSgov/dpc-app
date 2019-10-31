package gov.cms.dpc.consent.exceptions;

public class InvalidSuppressionRecordException extends RuntimeException {

    private static final long serialVersionUID = 4915354561823017049L;

    public InvalidSuppressionRecordException(String message) {
        super(message);
    }

    public InvalidSuppressionRecordException(String message, Throwable exception) {
        super(message, exception);
    }
}
