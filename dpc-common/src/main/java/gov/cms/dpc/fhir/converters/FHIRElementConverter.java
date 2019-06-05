package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.instance.model.api.IBaseElement;

/**
 * Converts a FHIR {@link org.hl7.fhir.instance.model.api.IBaseElement} into a corresponding Java class
 *
 * @param <D> - {@link org.hl7.fhir.instance.model.api.IBaseElement} to convert
 * @param <C> - Java class to convert to
 * @throws gov.cms.dpc.fhir.exceptions.DataTranslationException if data is missing or invalid
 */
@FunctionalInterface
public interface FHIRElementConverter<E extends IBaseElement, C> {

    C convert(E element);
}
