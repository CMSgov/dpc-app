package gov.cms.dpc.testing.exceptions;

/**
 * To be thrown during testing when a System.exit() is called.
 */
public class SystemExitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SystemExitException(String errorMessage) {
        super(errorMessage);
    }
}