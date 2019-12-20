package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.BaseResource;

/**
 * Converts a FHIR {@link BaseResource} into a corresponding Java class
 *
 * @param <R> - {@link BaseResource} to convert
 * @param <C> - Java class to convert to
 */
@FunctionalInterface
public interface FHIRResourceConverter<R extends Base, C> {

    /**
     * Convert FHIR {@link BaseResource} into a corresponding Java class
     *
     * @param resource - {@link BaseResource} to convert
     * @return - {@link C} Java class to convert to
     * @throws gov.cms.dpc.fhir.exceptions.DataTranslationException if data is missing or invalid
     */
    C convert(R resource);
}
