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
import gov.cms.dpc.fhir.validations.DPCValidationModule;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;

import javax.inject.Singleton;

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

        // Validator
        bind(DPCValidationModule.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    @Singleton
    FhirValidator provideValidator(FhirContext ctx, DPCValidationModule dpcModule) {
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();
        final FhirValidator fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(true);
        fhirValidator.setValidateAgainstStandardSchema(true);
        fhirValidator.registerValidatorModule(instanceValidator);

        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);

        return fhirValidator;
    }
}
