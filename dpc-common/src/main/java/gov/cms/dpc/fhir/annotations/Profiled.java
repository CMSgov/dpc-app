package gov.cms.dpc.fhir.annotations;

import gov.cms.dpc.fhir.validations.r4.ProfileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Any resource using @Profiled must have a corresponding profile in
 * {@link gov.cms.dpc.fhir.validations.v1.ProfileValidator} or {@link ProfileValidator}.
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = {gov.cms.dpc.fhir.validations.v1.ProfileValidator.class, ProfileValidator.class})
@Documented
@Repeatable(Profiled.List.class)
public @interface Profiled {

    String message() default "{org.hibernate.validator.referenceguide.chapter06.CheckCase." +
            "message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface List {
        Profiled[] value();
    }
}
