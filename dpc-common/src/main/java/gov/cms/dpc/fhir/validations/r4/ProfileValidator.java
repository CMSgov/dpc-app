package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.r4.OrganizationProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Organization;

public class ProfileValidator implements ConstraintValidator<Profiled, BaseResource> {

    private final FhirValidator validator;

    @Inject
    public ProfileValidator(@Named("r4") FhirValidator validator) {
        this.validator = validator;
    }

    @Override
    public boolean isValid(BaseResource value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (value == null || !value.isResource()) {
            context.buildConstraintViolationWithTemplate("No resource provided").addConstraintViolation();
            return false;
        }
        if (!(value instanceof Organization)) {
            throw new IllegalArgumentException("No R4 profile configured for " + value.getClass().getName());
        }

        final ValidationOptions options = new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI);
        final var result = validator.validateWithResult(value, options);
        if (result.isSuccessful()) {
            return true;
        }

        result.getMessages().forEach(message -> context
                .buildConstraintViolationWithTemplate("{gov.cms.dpc.fhir.validations.ProfileValidator." + message.getMessage() + "}")
                .addConstraintViolation());
        return false;
    }
}
