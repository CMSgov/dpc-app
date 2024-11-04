package gov.cms.dpc.fhir.validations.dropwizard;

import io.dropwizard.jersey.validation.DropwizardConfiguredValidator;

import jakarta.inject.Inject;
import jakarta.validation.Validator;

public class InjectingConfiguredValidator extends DropwizardConfiguredValidator {

    @Inject
    public InjectingConfiguredValidator( @jakarta.inject.Named("FhirVal") Validator validator) {
        super(validator);
    }
}
