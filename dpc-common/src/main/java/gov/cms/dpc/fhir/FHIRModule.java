package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
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
import gov.cms.dpc.fhir.parameters.ProvenanceResourceFactoryProvider;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidationModule;
import io.dropwizard.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

import static gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;

import jakarta.ws.rs.container.ResourceInfo;  // Make sure this import is in place

public class FHIRModule<T extends Configuration & IDPCFHIRConfiguration> extends DropwizardAwareModule<T> {

    private static final Logger logger = LoggerFactory.getLogger(FHIRModule.class);

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Override
    public void configure() {

        logger.info("Configuring FHIRModule...");

        Binder binder = binder();

        // Ensure ProvenanceResourceFactoryProvider is bound correctly
        binder.bind(ProvenanceResourceFactoryProvider.class).in(Singleton.class); 

        logger.info("ProvenanceResourceFactoryProvider has been bound.");
        
        
        // Bind ResourceInfo to make it available for injection
        binder.bind(ResourceInfo.class).toProvider(() -> {

            // Returning null here because ResourceInfo will only be available at request time
            logger.info("Returning null for ResourceInfo provider");
            return null;
        }).in(Singleton.class);  // This may also be RequestScoped if ResourceInfo has per-request data
        logger.info("ResourceInfo has been bound.");

        // Other bindings...
        logger.info("Binding other components...");
        binder.bind(FHIRHandler.class);
        binder.bind(BundleHandler.class);
        binder.bind(FHIRRequestFeature.class);
        binder.bind(FHIRParamValueFactory.class);
        binder.bind(StreamingContentSizeFilter.class);

        // Custom exception mappers
        binder.bind(JerseyExceptionHandler.class);
        binder.bind(PersistenceExceptionHandler.class);
        binder.bind(HAPIExceptionHandler.class);
        binder.bind(DefaultFHIRExceptionHandler.class);
//        binder.bind(ProvenanceResourceFactoryProvider.class);
        logger.info("Exception handlers have been bound.");

        binder.bind(FHIREntityConverter.class).toProvider(EntityConverterProvider.class).in(Singleton.class);
        logger.info("FHIRENtityConverter bound.");

        // Validator
        final FHIRValidationConfiguration validationConfig = configuration().getFHIRConfiguration().getValidation();
        if (validationConfig.isEnabled()) {
            logger.info("Enabling FHIR resource validation");
            binder.install(new FHIRValidationModule(validationConfig));
        } else {
            logger.info("Not enabling FHIR resource validation");
        }
    }
}
