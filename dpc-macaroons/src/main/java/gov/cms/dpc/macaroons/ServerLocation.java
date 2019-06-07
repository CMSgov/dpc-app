package gov.cms.dpc.macaroons;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting the location of the current server, to use when creating {@link com.github.nitram509.jmacaroons.Macaroon}s
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@BindingAnnotation
public @interface ServerLocation {
}
