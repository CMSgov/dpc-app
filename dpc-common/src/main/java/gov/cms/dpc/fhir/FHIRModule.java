package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.MethodOutcomeHandler;
import gov.cms.dpc.fhir.validations.DPCValidationModule;
import org.hl7.fhir.dstu3.hapi.ctx.IValidationSupport;

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
        bind(FHIRRequestFeature.class);

        // Validator
        bind(IValidationSupport.class).to(DPCValidationModule.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }
}
