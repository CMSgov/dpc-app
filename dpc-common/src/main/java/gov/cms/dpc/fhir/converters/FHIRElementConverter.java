package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.instance.model.api.IBaseElement;

/**
 * Converts a FHIR {@link org.hl7.fhir.instance.model.api.IBaseElement} into a corresponding Java class
 *
 * @param <E> - {@link org.hl7.fhir.instance.model.api.IBaseElement} to convert
 * @param <C> - Java class to convert to
 */
@FunctionalInterface
public interface FHIRElementConverter<E extends IBaseElement, C> {

    /**
     * Convert FHIR {@link IBaseElement} into a corresponding Java class
     *
     * @param element - {@link IBaseElement} to convert
     * @return - {@link C} Java class to convert to
     * @throws gov.cms.dpc.fhir.exceptions.DataTranslationException if data is missing or invalid
     */
    C convert(E element);
}
