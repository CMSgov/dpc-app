package gov.cms.dpc.api.auth.annotations;

import org.hl7.fhir.dstu3.model.ResourceType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Security} annotation which indicates that a given path component must be used to match security against the provided token
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Security
public @interface PathAuthorizer {
    ResourceType type();
    String pathParam();
}
