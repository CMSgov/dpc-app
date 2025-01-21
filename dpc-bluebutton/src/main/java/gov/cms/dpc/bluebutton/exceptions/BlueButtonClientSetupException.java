package gov.cms.dpc.bluebutton.exceptions;

public class BlueButtonClientSetupException extends RuntimeException {

    private static final long serialVersionUID = 42L;

    public BlueButtonClientSetupException(String message, Throwable cause){
        super(message, cause);
    }
}
