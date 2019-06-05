package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Converts a FHIR {@link IBaseResource} into a corresponding Java class
 *
 * @param <R> - {@link IBaseResource} to convert
 * @param <C> - Java class to convert to
 * @throws gov.cms.dpc.fhir.exceptions.DataTranslationException if data is missing or invalid
 */
@FunctionalInterface
public interface FHIRResourceConverter<R extends IBaseResource, C> {

    C convert(R resource);
}
