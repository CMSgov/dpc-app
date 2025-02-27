package gov.cms.dpc.fhir.validations.dropwizard;

import io.dropwizard.jersey.validation.DropwizardConfiguredValidator;

import javax.inject.Inject;
import javax.validation.Validator;

public class InjectingConfiguredValidator extends DropwizardConfiguredValidator {

    @Inject
    public InjectingConfiguredValidator(Validator validator) {
        super(validator);
    }
}
