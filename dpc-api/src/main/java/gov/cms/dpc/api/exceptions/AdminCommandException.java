package gov.cms.dpc.api.exceptions;

public class AdminCommandException extends RuntimeException {
    private final Integer statusCode;

    public AdminCommandException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AdminCommandException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public AdminCommandException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
