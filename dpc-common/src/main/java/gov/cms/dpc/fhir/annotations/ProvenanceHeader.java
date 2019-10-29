package gov.cms.dpc.fhir.annotations;

import javax.validation.Valid;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which specifies that the necessary {@link org.hl7.fhir.dstu3.model.Provenance} resource should be found in the 'X-PROVENANCE' header of the request
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ProvenanceHeader {
}
