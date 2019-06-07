package gov.cms.dpc.macaroons.exceptions;

public class BakeryException extends RuntimeException {
    public static final long serialVersionUID = 42L;

    public BakeryException(String message) {
        super(message);
    }
}
