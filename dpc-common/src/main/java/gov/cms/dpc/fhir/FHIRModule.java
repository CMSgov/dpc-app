package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.MethodOutcomeHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.HAPIExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.JerseyExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.PersistenceExceptionHandler;
import gov.cms.dpc.fhir.parameters.FHIRParamValueFactory;
import gov.cms.dpc.fhir.parameters.ProvenanceResourceValueFactory;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidationModule;
import io.dropwizard.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import static gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;

public class FHIRModule<T extends Configuration & IDPCFHIRConfiguration> extends DropwizardAwareModule<T> {

    private static final Logger logger = LoggerFactory.getLogger(FHIRModule.class);

    public FHIRModule() {
        // Not used
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Override
    public void configure(Binder binder) {
        // Request/Response handlers
        binder.bind(FHIRHandler.class);
        binder.bind(MethodOutcomeHandler.class);
        binder.bind(FHIRRequestFeature.class);
        binder.bind(FHIRParamValueFactory.class);

        // Custom exception mappers
        binder.bind(JerseyExceptionHandler.class);
        binder.bind(PersistenceExceptionHandler.class);
        binder.bind(HAPIExceptionHandler.class);
        binder.bind(DefaultFHIRExceptionHandler.class);
        binder.bind(FHIRParamValueFactory.class);
        binder.bind(ProvenanceResourceValueFactory.class);

        // Validator
        final FHIRValidationConfiguration validationConfig = getConfiguration().getFHIRConfiguration().getValidation();
        if (validationConfig.isEnabled()) {
            logger.info("Enabling FHIR resource validation");
            binder.install(new FHIRValidationModule(validationConfig));
        } else {
            logger.info("Not enabling FHIR resource validation");
        }
    }
}
