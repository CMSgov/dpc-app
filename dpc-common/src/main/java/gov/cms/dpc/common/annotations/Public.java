package gov.cms.dpc.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Security} annotation which indicates this resource (or set of resources) is public and requires no authentication
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Security
public @interface Public {
}
