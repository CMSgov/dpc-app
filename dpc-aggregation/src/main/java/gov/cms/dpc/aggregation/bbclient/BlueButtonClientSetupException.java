package gov.cms.dpc.aggregation.bbclient;

public class BlueButtonClientSetupException extends RuntimeException {

    public static final long serialVersionUID = 42L;

    public BlueButtonClientSetupException(String message, Throwable cause){
        super(message, cause);
    }
}
