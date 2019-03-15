package gov.cms.dpc.api.annotations;

import com.google.inject.BindingAnnotation;
import gov.cms.dpc.common.interfaces.AttributionEngine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a web target to the remote {@link AttributionEngine}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@BindingAnnotation
public @interface AttributionService {
}
