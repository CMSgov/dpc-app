package gov.cms.dpc.attribution.exceptions;

public class AttributionException extends RuntimeException {
    public static final long serialVersionUID = 42L;

    public AttributionException(String message) {
        super(message);
    }

    public AttributionException(String message, Throwable exception) {
        super(message, exception);
    }
}
