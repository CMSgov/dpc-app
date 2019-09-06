package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

public class ValidationHelpers {

    private ValidationHelpers() {
        // Not used
    }

    /**
     * Validates the provided {@link Resource} against the given Profile
     *
     * @param validator  - {@link FhirValidator} to use for validation
     * @param params     - {@link Parameters} to get resource from
     * @param profileURL - {@link String} profile URL to use for validation
     * @return - {@link IBaseOperationOutcome} outcome with failures (if any)
     */
    public static IBaseOperationOutcome validateAgainstProfile(FhirValidator validator, Parameters params, String profileURL) {
        final Resource resource = params.getParameterFirstRep().getResource();

        final ValidationOptions valOps = new ValidationOptions();
        final ValidationResult validationResult = validator.validateWithResult(resource, valOps.addProfile(profileURL));
        return validationResult.toOperationOutcome();
    }
}
