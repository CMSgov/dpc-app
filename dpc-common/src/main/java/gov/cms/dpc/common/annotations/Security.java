package gov.cms.dpc.common.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Parent annotation for specifying desired security properties.
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface Security {
}
