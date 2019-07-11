package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.validation.FhirValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Guice module for setting up the required Validation components, if requested by the application
 */
public class FHIRValidationModule extends AbstractModule {

    public FHIRValidationModule() {
        // Not used
    }


    @Override
    protected void configure() {

        TypeLiteral<Set<ProfileValidator>> constraintType = new TypeLiteral<>() {
        };
        
        bind(constraintType).toProvider(ConstraintValidationProvider.class);
        bind(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class);
        bind(ConstraintValidatorFactory.class).to(InjectingConstraintValidatorFactory.class);
        bind(ValidationConfigurationContextResolver.class);
        bind(FHIRValidationExceptionHandler.class);

        bind(DPCProfileSupport.class);
        bind(FhirValidator.class).toProvider(FHIRValidatorProvider.class).in(Scopes.SINGLETON);
    }
}
