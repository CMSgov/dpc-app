package gov.cms.dpc.fhir;

import org.hl7.fhir.dstu3.model.Resource;

/**
 * Interface for converting to and from a FHIR {@link Resource}
 * Should throw a {@link ca.uhn.fhir.parser.DataFormatException} if either of the objects are missing data or invalid
 *
 * @param <O> - {@link O} Java object to convert to/from FHIR {@link Resource}
 * @param <R> - {@link R} FHIR {@link Resource} to convert to/from Java object
 */
public interface FHIRConvertable<O, R extends Resource> {

    /**
     * Converts FHIR {@link Resource} to Java object
     *
     * @param resource - {@link Resource} to convert to Java object
     * @return - {@link O} Java object converted from FHIR {@link Resource} {@link R}
     */
    O fromFHIR(R resource);

    /**
     * Converts Java object to FHIR {@link Resource}
     *
     * @return - {@link R} FHIR {@link Resource} created from Java object {@link O}
     */
    R toFHIR();
}
