package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.validations.ProfileValidator;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

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
