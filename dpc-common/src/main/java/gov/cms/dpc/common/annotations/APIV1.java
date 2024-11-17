package gov.cms.dpc.common.annotations;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates the API returns data from a V1 resource.
 * This extends the {@link ServiceBaseURL} by appending /v1 to the end.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Qualifier
public @interface APIV1 {
}
