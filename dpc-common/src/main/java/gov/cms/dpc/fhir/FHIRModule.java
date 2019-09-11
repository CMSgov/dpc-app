package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.MethodOutcomeHandler;
import gov.cms.dpc.fhir.paramtests.ParamFactory;
import gov.cms.dpc.fhir.paramtests.ParamValueFactory;
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
        // Request/Response handlers
        binder.bind(FHIRExceptionHandler.class);
        binder.bind(FHIRRequestFeature.class);

//        binder.bind(FHIRParamValueFactoryProvider.class);
//        binder.bind(GuiceFeature.class).asEagerSingleton();
//        binder.bind(ContextProvider.class);
//        binder.bind(ParamFactory.class);
        binder.bind(ParamValueFactory.class);

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
