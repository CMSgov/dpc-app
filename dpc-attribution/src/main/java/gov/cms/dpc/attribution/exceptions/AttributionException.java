package gov.cms.dpc.attribution.exceptions;

import java.io.Serial;

public class AttributionException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 42L;

    public AttributionException(String message) {
        super(message);
    }

    public AttributionException(String message, Throwable exception) {
        super(message, exception);
    }
}
