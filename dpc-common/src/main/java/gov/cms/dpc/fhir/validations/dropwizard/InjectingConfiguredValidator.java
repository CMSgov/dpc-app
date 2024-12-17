package gov.cms.dpc.fhir.validations.dropwizard;

import com.google.inject.Inject;
import io.dropwizard.jersey.validation.DropwizardConfiguredValidator;
import jakarta.validation.Validator;

public class InjectingConfiguredValidator extends DropwizardConfiguredValidator {

    @Inject
    public InjectingConfiguredValidator(Validator validator) {
        super(validator);
    }
}
