package gov.cms.dpc.bluebuttonv2.exceptions;

public class BlueButtonClientSetupExceptionV2 extends RuntimeException {

    public static final long serialVersionUID = 42L;

    public BlueButtonClientSetupExceptionV2(String message, Throwable cause){
        super(message, cause);
    }
}
