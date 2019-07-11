package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.validations.FHIRProfileValidator;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class FHIRValidatorProvider implements Provider<FhirValidator> {

    private static final Logger logger = LoggerFactory.getLogger(FHIRValidatorProvider.class);

    private final FhirContext ctx;
    private final FHIRProfileValidator dpcModule;
    private final DPCFHIRConfiguration.FHIRValidationConfiguration validationConfiguration;

    @Inject
    public FHIRValidatorProvider(FhirContext ctx, FHIRProfileValidator dpcModule, DPCFHIRConfiguration config) {
        this.ctx = ctx;
        this.dpcModule = dpcModule;
        this.validationConfiguration = config.getValidation();
    }


    @Override
    public FhirValidator get() {
        logger.debug("Providing FHIR validator.");
        logger.debug("Schema validation enabled: {}.\nSchematron validation enabled: {}", validationConfiguration.isSchemaValidation(), validationConfiguration.isSchematronValidation());
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();
        final FhirValidator fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(validationConfiguration.isSchematronValidation());
        fhirValidator.setValidateAgainstStandardSchema(validationConfiguration.isSchemaValidation());
        fhirValidator.registerValidatorModule(instanceValidator);

        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);

        return fhirValidator;
    }
}
