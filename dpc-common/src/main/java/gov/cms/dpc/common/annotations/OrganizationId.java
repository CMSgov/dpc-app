package gov.cms.dpc.common.annotations;

import gov.cms.dpc.common.hibernate.validator.Stu3OrganizationValidator;
import gov.cms.dpc.common.hibernate.validator.R4OrganizationIdValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {Stu3OrganizationValidator.class, R4OrganizationIdValidator.class})
@Documented
public @interface OrganizationId {

    String message() default Stu3OrganizationValidator.VALIDATION_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
