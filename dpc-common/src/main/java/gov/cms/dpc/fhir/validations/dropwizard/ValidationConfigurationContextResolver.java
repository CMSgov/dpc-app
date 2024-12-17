package gov.cms.dpc.fhir.validations.dropwizard;

import com.google.inject.Inject;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.validation.ValidationConfig;

/**
 * Custom {@link ContextResolver} that allows us to override the default Hibernate/Dropwizard settings in order to support injection for our custom {@link jakarta.validation.ConstraintValidator}
 * <p>
 * This should be natively supported in Dropwizard 2.0, but until then, we need this.
 */
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
