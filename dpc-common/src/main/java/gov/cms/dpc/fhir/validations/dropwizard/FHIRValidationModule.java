package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import com.google.inject.Singleton;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FHIRValidationModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRValidationModule.class);

    private final FHIRValidationConfiguration config;

    public FHIRValidationModule(FHIRValidationConfiguration config) {
        this.config = config;
    }
    
    public static final AtomicBoolean BOUND = new AtomicBoolean(false);

    @Override
    protected void configure() {
        LOG.info("Configure is running!");

        bind(ConstraintValidatorFactory.class).to(InjectingConstraintValidatorFactory.class);
        bind(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class).in(Scopes.SINGLETON);
        bind(ConfiguredValidator.class).to(InjectingConfiguredValidator.class);
        
        if(BOUND.compareAndSet(false, true))
            bind(FhirValidator.class).toProvider(FHIRValidatorProvider.class).asEagerSingleton();
        bind(DPCProfileSupport.class).in(Scopes.SINGLETON);
    }
    
    @Provides
    @Singleton
    public Set<ConstraintValidator<?, ?>> provideConstraintValidators(FhirValidator fhirValidator) {
        Set<ConstraintValidator<?, ?>> validators = new HashSet<>();
        validators.add(new ProfileValidator(fhirValidator));
        return validators;
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