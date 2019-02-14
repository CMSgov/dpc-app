package gov.cms.dpc.fhir;

import com.google.inject.AbstractModule;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;

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
}
