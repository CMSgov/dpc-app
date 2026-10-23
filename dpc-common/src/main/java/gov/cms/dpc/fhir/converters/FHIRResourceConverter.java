package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.fhir.converters.exceptions.DataTranslationException;
import org.hl7.fhir.instance.model.api.IBase;

/**
 * Converts a FHIR {@link IBase} into a corresponding Java class
 *
 * @param <R> - {@link IBase} to convert
 * @param <C> - Java class to convert to
 */
@FunctionalInterface
public interface FHIRResourceConverter<R extends IBase, C> {

    /**
     * Convert FHIR {@link IBase} into a corresponding Java class
     *
     * @param resource - {@link IBase} to convert
     * @return - {@link C} Java class to convert to
     * @throws DataTranslationException if data is missing or invalid
     */
    C convert(R resource);
}
