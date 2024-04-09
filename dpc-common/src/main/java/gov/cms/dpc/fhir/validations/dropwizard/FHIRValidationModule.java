package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.validation.FhirValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * Guice module for setting up the required Validation components, if requested by the application
 */
public class FHIRValidationModule extends AbstractModule {

    private final FHIRValidationConfiguration config;
    private static final Logger logger = LoggerFactory.getLogger(FHIRValidationModule.class);

    public FHIRValidationModule(FHIRValidationConfiguration config) {
        this.config = config;
    }


    @Override
    protected void configure() {
        // This is required for Guice to load correctly.
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();

        // Create a multi-binder for automatically bundling and injecting a Set of ConstraintValidators
        TypeLiteral<ConstraintValidator<?, ?>> constraintType = new TypeLiteral<>() {
        };
        Multibinder<ConstraintValidator<?, ?>> constraintBinder = Multibinder.newSetBinder(binder(), constraintType);
        constraintBinder.addBinding().to(ProfileValidator.class);

        bind(ConstraintValidatorFactory.class).to(InjectingConstraintValidatorFactory.class);
        bind(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class);
        bind(ConfiguredValidator.class).to(InjectingConfiguredValidator.class);

        bind(DPCProfileSupport.class).in(Scopes.SINGLETON);
        bind(FhirValidator.class).toProvider(FHIRValidatorProvider.class);
    }

    @Provides
    FHIRValidationConfiguration provideValidationConfig() {
        return this.config;
    }

    @Provides
    Validator provideValidator(ValidatorFactory factory) {
        return factory.getValidator();
    }

    @Provides
    ValidationSupportChain provideSupportChain(DPCProfileSupport dpcModule) {
        return new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
    }
}
