package gov.cms.dpc.fhir.validations.dropwizard;

import org.glassfish.jersey.server.validation.ValidationConfig;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidatorFactory;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ValidationConfigurationContextResolver implements ContextResolver<ValidationConfig> {

    private final ConstraintValidatorFactory constraintValidatorFactory;
    private final ValidatorFactory factory;

    @Inject
    public ValidationConfigurationContextResolver(ConstraintValidatorFactory constraintValidatorFactory, ValidatorFactory factory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
        this.factory = factory;
    }

    @Override
    public ValidationConfig getContext(final Class<?> type) {
        final ValidationConfig config = new ValidationConfig();
        config.messageInterpolator(factory.getMessageInterpolator());
        config.constraintValidatorFactory(constraintValidatorFactory); // custom constraint validator factory
        config.parameterNameProvider(factory.getParameterNameProvider());
        config.traversableResolver(factory.getTraversableResolver());
        return config;
    }
}
