package gov.cms.dpc.macaroons.exceptions;

import java.io.Serial;

/**
 * Generic exception thrown by the Bakery when something's not right.
 * // TODO: Expand this to be more general purpose
 */
public class BakeryException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 42L;

    public BakeryException(String message) {
        super(message);
    }

    public BakeryException(String message, Exception e) {
        super(message, e);
    }
}
