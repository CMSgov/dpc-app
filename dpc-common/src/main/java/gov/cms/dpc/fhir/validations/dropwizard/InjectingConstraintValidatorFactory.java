package gov.cms.dpc.fhir.validations.dropwizard;

import com.google.inject.Injector;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

/**
 * Custom {@link ConstraintValidatorFactory} that allows us to inject services
 * into our custom {@link ConstraintValidator}.
 * <p>
 * If a custom validator isn't present, it delegates to the underlying
 * {@link ConstraintValidatorFactory} and tries to load from there.
 */
public class InjectingConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Inject
    private Injector injector;

    public InjectingConstraintValidatorFactory(Set<ConstraintValidator<?, ?>> validators) {
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
        /*
         * By default, all beans are in prototype scope, so new instance will be
         * obtained each time.
         * Validator implementer may declare it as singleton and manually maintain
         * internal state
         * (to re-use validators and simplify life for GC)
         */
        return injector.getInstance(key);
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
    }
}
