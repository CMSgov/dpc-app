package gov.cms.dpc.fhir.validations.dropwizard;

import io.dropwizard.jersey.validation.DropwizardConfiguredValidator;
import jakarta.validation.Validator;
import jakarta.inject.Inject;

public class InjectingConfiguredValidator extends DropwizardConfiguredValidator {

    @Inject
    public InjectingConfiguredValidator(Validator validator) {
        super(validator);
    }
}
