package gov.cms.dpc.consent.exceptions;

public class InvalidSuppressionRecordException extends RuntimeException {

    public InvalidSuppressionRecordException(String message) {
        super(message);
    }

    public InvalidSuppressionRecordException(String message, Throwable exception) {
        super(message, exception);
    }
}
