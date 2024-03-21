package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.*;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;

/**
 * Hibernate {@link ConstraintValidator} that provides support for running the {@link FhirValidator} against FHIR resources, as part of the normal Dropwizard validation framework.
 * This requires that the {@link Profiled} annotation be added to the resource.
 */
public class ProfileValidator implements ConstraintValidator<Profiled, BaseResource> {

    private static final String VALIDATION_CONSTANT = "{gov.cms.dpc.fhir.validations.ProfileValidator.";
    private final FhirValidator validator;
    private Map<Class<?>, String> resourceProfileMap;

    @Inject
    public ProfileValidator(FhirValidator validator) {
        this.validator = validator;
    }

    @Override
    public void initialize(Profiled constraintAnnotation) {
        this.resourceProfileMap = setProfilesForResources();
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

        // Create a validation option object which forces validation against the given profile.
        final ValidationOptions options = new ValidationOptions();
        options.addProfile(resourceProfileMap.getOrDefault(value.getClass(), ""));
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

    // This map must be updated if we need to use profiles for any other resources.
    private Map<Class<?>, String> setProfilesForResources() {
        return Map.of(
            Endpoint.class, EndpointProfile.PROFILE_URI,
            Organization.class, OrganizationProfile.PROFILE_URI,
            Patient.class, PatientProfile.PROFILE_URI,
            Practitioner.class, PractitionerProfile.PROFILE_URI,
            Provenance.class, AttestationProfile.PROFILE_URI
        );
    }
}
