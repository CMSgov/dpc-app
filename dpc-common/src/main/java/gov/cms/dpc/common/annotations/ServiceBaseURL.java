package gov.cms.dpc.common.annotations;

import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Root URL of the given Dropwizard service.
 * This should be generated from the HTTP Request host/port combination.
 * The injection can be extended by using the {@link APIV1} annotation to append the /v1 suffix to the end of the URL.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface ServiceBaseURL {
}
