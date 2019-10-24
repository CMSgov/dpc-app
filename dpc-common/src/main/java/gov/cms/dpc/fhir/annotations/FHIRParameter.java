package gov.cms.dpc.fhir.annotations;

import org.hl7.fhir.dstu3.model.Parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which specifies that the annotated resource is part of a FHIR {@link org.hl7.fhir.dstu3.model.Parameters} object.
 * By default, it automatically call {@link Parameters#getParameterFirstRep()} and return the resource
 * <p>
 * Users can specify which parameter to use by setting the {@link FHIRParameter#name()} value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FHIRParameter {

    String name() default "";
}
