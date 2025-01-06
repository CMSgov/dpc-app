package gov.cms.dpc.api.exceptions;

import java.io.Serial;

public class PublicKeyException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 42L;

    public PublicKeyException(String message) {
        super(message);
    }

    public PublicKeyException(String message, Throwable e) {
        super(message, e);
    }
}
