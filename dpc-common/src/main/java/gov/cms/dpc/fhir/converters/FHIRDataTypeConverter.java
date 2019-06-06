package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.instance.model.api.IBaseDatatype;

/**
 * Converts a FHIR {@link IBaseDatatype} into a corresponding Java class
 *
 * @param <D> - {@link IBaseDatatype} to convert
 * @param <C> - Java class to convert to
 */
@FunctionalInterface
public interface FHIRDataTypeConverter<D extends IBaseDatatype, C> {

    /**
     * Convert FHIR {@link IBaseDatatype} into a corresponding Java class
     *
     * @param datatype - {@link IBaseDatatype} to convert
     * @return - {@link C} Java class to convert to
     * @throws gov.cms.dpc.fhir.exceptions.DataTranslationException if data is missing or invalid
     */
    C convert(D datatype);
}
