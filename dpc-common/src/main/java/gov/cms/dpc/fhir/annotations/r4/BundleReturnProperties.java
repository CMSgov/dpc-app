package gov.cms.dpc.fhir.annotations.r4;

import org.hl7.fhir.r4.model.Bundle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying custom properties for {@link Bundle} resources generated from returning collections.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BundleReturnProperties {

    Bundle.BundleType bundleType() default Bundle.BundleType.SEARCHSET;
}
