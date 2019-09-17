package gov.cms.dpc.api.exceptions;

public class PublicKeyException extends RuntimeException {
    public static final long serialVersionUID = 42L;

    public PublicKeyException(String message) {
        super(message);
    }

    public PublicKeyException(String message, Throwable e) {
        super(message, e);
    }
}
