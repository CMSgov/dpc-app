package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Provides;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.dropwizard.features.FHIRRequestFeature;
import gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter;
import gov.cms.dpc.fhir.dropwizard.handlers.BundleHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.HAPIExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.JerseyExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.PersistenceExceptionHandler;
import gov.cms.dpc.fhir.parameters.FHIRParamValueFactory;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidationModule;
import io.dropwizard.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

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
    public void configure() {
        // Request/Response handlers
        binder().bind(FHIRHandler.class);
        binder().bind(BundleHandler.class);
        binder().bind(FHIRRequestFeature.class);
        binder().bind(FHIRParamValueFactory.class);
        binder().bind(StreamingContentSizeFilter.class);

        // Custom exception mappers
        binder().bind(JerseyExceptionHandler.class);
        binder().bind(PersistenceExceptionHandler.class);
        binder().bind(HAPIExceptionHandler.class);
        binder().bind(DefaultFHIRExceptionHandler.class);
        binder().bind(FHIRParamValueFactory.class);

        binder().bind(FHIREntityConverter.class).toProvider(EntityConverterProvider.class).in(Singleton.class);

        // Validator
        final FHIRValidationConfiguration validationConfig = configuration().getFHIRConfiguration().getValidation();
        if (validationConfig.isEnabled()) {
            logger.info("Enabling FHIR resource validation");
            binder().install(new FHIRValidationModule(validationConfig));
        } else {
            logger.info("Not enabling FHIR resource validation");
        }
    }
}
