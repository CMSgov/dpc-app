package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.validations.ProfileValidator;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

/**
 * Custom {@link Provider} that creates a {@link Set} of our custom {@link javax.validation.ConstraintValidator}.
 * <p>
 * Eventually, this will be moved to a {@link com.google.inject.multibindings.Multibinder}, but for now, we just do things manually.
 */
public class ConstraintValidationProvider implements Provider<Set<ProfileValidator>> {

    private final FhirValidator validator;

    @Inject
    ConstraintValidationProvider(FhirValidator validator) {
        this.validator = validator;

    }

    @Override
    public Set<ProfileValidator> get() {
        return Set.of(new ProfileValidator(validator));
    }
}
