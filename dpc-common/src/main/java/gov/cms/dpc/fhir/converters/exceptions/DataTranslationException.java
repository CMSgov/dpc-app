package gov.cms.dpc.fhir.converters.exceptions;

import java.io.Serial;

public class DataTranslationException extends FHIRConverterException {
    @Serial
    private static final long serialVersionUID = 42L;

    private final Class<?> clazz;
    private final String element;

    public DataTranslationException(Class<?> clazz, String element, String message) {
        super(String.format("Class: %s. Element: %s. %s", clazz.getName(), element, message));
        this.element = element;
        this.clazz = clazz;
    }

    public String getElement() {
        return element;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
