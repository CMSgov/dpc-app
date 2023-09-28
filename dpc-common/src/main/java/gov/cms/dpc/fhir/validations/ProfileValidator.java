package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.annotations.Profiled;
import org.hl7.fhir.dstu3.model.BaseResource;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Hibernate {@link ConstraintValidator} that provides support for running the
 * {@link FhirValidator} against FHIR resources, as part of the normal
 * Dropwizard validation framework.
 * This requires that the {@link Profiled} annotation be added to the resource.
 */
public class ProfileValidator implements ConstraintValidator<Profiled, BaseResource> {

    private static final String VALIDATION_CONSTANT = "{gov.cms.dpc.fhir.validations.ProfileValidator.";
    private final FhirValidator validator;
    private String profileURI;

    @Inject
    public ProfileValidator(FhirValidator validator) {
        this.validator = validator;
    }

    @Override
    public void initialize(Profiled constraintAnnotation) {
        this.profileURI = constraintAnnotation.profile();
    }

    @Override
    public boolean isValid(BaseResource value, ConstraintValidatorContext context) {
        // Disable default error messages, as we want to generate our own
        context.disableDefaultConstraintViolation();

        if (value == null || !value.isResource()) {
            context.buildConstraintViolationWithTemplate("No resource provided")
                    .addConstraintViolation();
            return false;
        }

        // Create a validation option object which forces validation against the given
        // profile.
        final ValidationOptions options = new ValidationOptions();
        options.addProfile(profileURI);
        final ValidationResult result = this.validator.validateWithResult(value, options);

        if (result.isSuccessful()) {
            return true;
        }

        // If we failed, tell us why
        result.getMessages()
                .forEach(msg -> context
                        .buildConstraintViolationWithTemplate(
                                VALIDATION_CONSTANT +
                                        msg.getMessage() + "}")
                        .addConstraintViolation());

        return false;
    }
}
