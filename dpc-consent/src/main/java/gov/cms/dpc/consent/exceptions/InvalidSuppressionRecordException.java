package gov.cms.dpc.consent.exceptions;

public class InvalidSuppressionRecordException extends RuntimeException {

    private static final long serialVersionUID = 4915354561823017049L;

    public InvalidSuppressionRecordException(String message, String filename, int lineNum) {
        super(String.format("%s: %s, %d", message, filename, lineNum));
    }

    public InvalidSuppressionRecordException(String message, String filename, int lineNum, Throwable exception) {
        super(String.format("%s: %s, %d", message, filename, lineNum), exception);
    }
}
