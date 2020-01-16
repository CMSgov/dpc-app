package gov.cms.dpc.fhir.converters.exceptions;

public class MissingConverterException extends FHIRConverterException {

    private static final long serialVersionUID = 42L;

    private final Class<?> sourceClass;
    private final Class<?> targetClass;

    public MissingConverterException(Class<?> source, Class<?> target) {
        super(String.format("Unable to find converter from %s to %s", source.getName(), target.getName()));
        this.sourceClass = source;
        this.targetClass = target;
    }

    public Class<?> getSourceClass() {
        return sourceClass;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }
}
