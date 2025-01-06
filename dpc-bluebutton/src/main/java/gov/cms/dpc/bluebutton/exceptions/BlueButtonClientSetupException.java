package gov.cms.dpc.bluebutton.exceptions;

import java.io.Serial;

public class BlueButtonClientSetupException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 42L;

    public BlueButtonClientSetupException(String message, Throwable cause){
        super(message, cause);
    }
}
