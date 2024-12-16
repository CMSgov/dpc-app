package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

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
        FhirContext ctx = FhirContext.forDstu3();
        return new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx));
    }
}
