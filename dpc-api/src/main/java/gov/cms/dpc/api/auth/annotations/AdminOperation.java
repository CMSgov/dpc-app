package gov.cms.dpc.api.auth.annotations;

import gov.cms.dpc.common.annotations.Security;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Security} annotation which indicates that a given operation is only accessible to admin tokens (e.g. Golden Macaroons)
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Security
public @interface AdminOperation {
}
