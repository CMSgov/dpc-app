package gov.cms.dpc.fhir.converters.exceptions;

/**
 * Base exception type for {@link gov.cms.dpc.fhir.converters.FHIRConverter}
 */
public class FHIRConverterException extends RuntimeException {
    private static final long serialVersionUID = 42L;

    private FHIRConverterException() {
        super();
    }

    public FHIRConverterException(String message) {
        super(message);
    }

    public FHIRConverterException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
