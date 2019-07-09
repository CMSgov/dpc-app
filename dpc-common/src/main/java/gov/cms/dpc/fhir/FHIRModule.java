package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.MethodOutcomeHandler;
import gov.cms.dpc.fhir.validations.*;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import gov.cms.dpc.fhir.validations.dropwizard.ValidationConfigurationContextResolver;

import javax.inject.Singleton;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class FHIRModule extends AbstractModule {

    public FHIRModule() {
//        Not used
    }

    @Override
    protected void configure() {
        // Request/Response handlers
        bind(FHIRHandler.class);
        bind(MethodOutcomeHandler.class);
        // Request/Response handlers
        bind(FHIRExceptionHandler.class);
        bind(FHIRValidationExceptionHandler.class);
        bind(FHIRRequestFeature.class);
        bind(InjectingConstraintValidatorFactory.class);
        bind(ConstraintValidatorFactory.class).to(InjectingConstraintValidatorFactory.class);
        bind(ValidationConfigurationContextResolver.class);

        // Validator
        bind(FHIRProfileValidator.class);
        bind(FhirValidator.class).toProvider(FHIRValidatorProvider.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    public ValidatorFactory provideValidatorFactory(InjectingConstraintValidatorFactory factory) {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(factory)
                .buildValidatorFactory();
    }

    @Provides
    Set<ConstraintValidator> provideValidators(FhirValidator validator) {
        return Set.of(new ProfileValidator(validator));
    }
}
