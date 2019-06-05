package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.instance.model.api.IBaseDatatype;

/**
 * Converts a FHIR {@link IBaseDatatype} into a corresponding Java class
 *
 * @param <D> - {@link IBaseDatatype} to convert
 * @param <C> - Java class to convert to
 * @throws gov.cms.dpc.fhir.exceptions.DataTranslationException if data is missing or invalid
 */
@FunctionalInterface
public interface FHIRDataTypeConverter<D extends IBaseDatatype, C> {

    C convert(D datatype);
}
