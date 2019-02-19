package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;

import javax.inject.Singleton;

public class FHIRModule extends AbstractModule {

    public FHIRModule() {
//        Not used
    }

    @Override
    protected void configure() {
        // Request/Response handlers
        bind(FHIRHandler.class);
        bind(FHIRExceptionHandler.class);
        bind(FHIRRequestFeature.class);
    }

    @Provides
    @Singleton
    public FhirContext provideContext() {
        return FhirContext.forR4();
    }
}
