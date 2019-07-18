package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.validation.FhirValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidatorFactory;

/**
 * Guice module for setting up the required Validation components, if requested by the application
 */
public class FHIRValidationModule extends AbstractModule {

    private final FHIRValidationConfiguration config;

    public FHIRValidationModule(FHIRValidationConfiguration config) {
        this.config = config;
    }


    @Override
    protected void configure() {

        // Create a multibinder for automatically bundling and injecting a Set of ConstraintValidators
        TypeLiteral<ConstraintValidator<?, ?>> constraintType = new TypeLiteral<>() {
        };
        Multibinder<ConstraintValidator<?, ?>> constraintBinder = Multibinder.newSetBinder(binder(), constraintType);
        constraintBinder.addBinding().to(ProfileValidator.class);

        bind(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class);
        bind(ConstraintValidatorFactory.class).to(InjectingConstraintValidatorFactory.class);
        bind(ValidationConfigurationContextResolver.class);
        bind(FHIRValidationExceptionHandler.class);

        bind(DPCProfileSupport.class).in(Scopes.SINGLETON);
        bind(FhirValidator.class).toProvider(FHIRValidatorProvider.class).in(Scopes.SINGLETON);
    }

    @Provides
    FHIRValidationConfiguration provideValidationConfig() {
        return this.config;
    }
}
