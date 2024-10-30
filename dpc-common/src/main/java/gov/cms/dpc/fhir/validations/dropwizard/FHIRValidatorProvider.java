package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.sql.Date;

import static gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;

public class FHIRValidatorProvider implements Provider<FhirValidator> {

    private static final Logger logger = LoggerFactory.getLogger(FHIRValidatorProvider.class);

    private volatile FhirValidator fhirValidator = null;
    private final FhirContext ctx;
    private final FHIRValidationConfiguration validationConfiguration;
    private final ValidationSupportChain supportChain;

    @Inject
    public FHIRValidatorProvider(FhirContext ctx, FHIRValidationConfiguration config, ValidationSupportChain supportChain) {
        this.ctx = ctx;
        this.validationConfiguration = config;
        this.supportChain = supportChain;
        
        logger.info("A FHIRValidatorProvider has been constructed!");
    }

    private FhirValidator initializeValidator() {
        logger.debug("Initializing FhirValidator with schema validation enabled");

        FhirValidator validator = ctx.newValidator();
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);
        validator.registerValidatorModule(instanceValidator);

        instanceValidator.setValidationSupport(supportChain);
        validator.setValidateAgainstStandardSchema(validationConfiguration.isSchemaValidation());

        // Prime the validator with a dummy patient if necessary
        ValidationOptions options = new ValidationOptions();
        options.addProfile(PatientProfile.PROFILE_URI);
        validator.validateWithResult(createDummyPatient(), options);

        logger.info("Initialization of FhirValidator complete!");
        return validator;
    }
 
    @Override
    public FhirValidator get() {
        FhirValidator fhirValidator = this.fhirValidator;

        // Lazy initialization with double-checked locking
        if (fhirValidator == null) {
            synchronized (this) {
                fhirValidator = this.fhirValidator;
                if (fhirValidator == null) {
                    this.fhirValidator = fhirValidator = initializeValidator();
                    logger.info("OK, lazily initialized FhirValidator!");
                }
            }
        }
        
        return fhirValidator;
    }

    /**
     * Helper method that primes the Validator cache by creating a dummy patient and validating it.
     * This is really dumb, but it avoids issues where the tests timeout when running in CI.
     * Is necessary in order to address DPC-608
     * <p>
     * We may need to add more resources here in the future, if things continue to be slow.
     *
     * @param validator - {@link FhirValidator} validator to prime
     */
    private Patient createDummyPatient() {
        logger.trace("Validating dummy patient");
        final Patient patient = new Patient();
        patient.addName().addGiven("Dummy").setFamily("Patient");
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(Date.valueOf("1990-01-01"));

        return patient;
    }
}