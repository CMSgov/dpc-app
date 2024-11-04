package gov.cms.dpc.fhir.validations.dropwizard;

import com.google.inject.name.Named;
import io.dropwizard.jersey.validation.DropwizardConfiguredValidator;

import jakarta.inject.Inject;
import jakarta.validation.Validator;

public class InjectingConfiguredValidator extends DropwizardConfiguredValidator {

    @Inject
    public InjectingConfiguredValidator(@Named("FHIRValidator") Validator validator) {
        super(validator);
    }
}
