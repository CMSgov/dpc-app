package gov.cms.dpc.fhir.validations.dropwizard;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Provide our custom {@link ValidatorFactory} that supports injecting resources into our custom {@link jakarta.validation.ConstraintValidator}
 */
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
