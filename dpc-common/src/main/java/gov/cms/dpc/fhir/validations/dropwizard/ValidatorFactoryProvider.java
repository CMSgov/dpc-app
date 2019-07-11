package gov.cms.dpc.fhir.validations.dropwizard;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class ValidatorFactoryProvider implements Provider<ValidatorFactory> {

    private final ConstraintValidatorFactory factoryProvider;

    @Inject
    ValidatorFactoryProvider(ConstraintValidatorFactory factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    @Override
    public ValidatorFactory get() {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(this.factoryProvider)
                .buildValidatorFactory();
    }
}
