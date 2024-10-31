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

    private static final Object lock = new Object();
    private static volatile boolean initialized = false;

    private final FhirContext ctx;
    private final FHIRValidationConfiguration validationConfiguration;
    private final ValidationSupportChain supportChain;

    @Inject
    @SuppressWarnings("StaticAssignmentInConstructor") // Needed to eagerly init the validator
    public FHIRValidatorProvider(FhirContext ctx, FHIRValidationConfiguration config, ValidationSupportChain supportChain) {
        this.ctx = ctx;
        this.validationConfiguration = config;
        this.supportChain = supportChain;

        // Double lock check to eagerly init the validator
        // Since we can't inject the provider as a singleton, we need a way to prime the validator on first use, but only once.
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    // Initialize
                    final FhirValidator fhirValidator = get();
                    initialize(fhirValidator);
                    initialized = true;
                }
            }
        }
    }


    @Override
    public FhirValidator get() {
        logger.debug("Schema validation enabled: {}", validationConfiguration.isSchemaValidation());
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);
        final FhirValidator fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchema(validationConfiguration.isSchemaValidation());
        fhirValidator.registerValidatorModule(instanceValidator);

        instanceValidator.setValidationSupport(this.supportChain);
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
    private static void initialize(FhirValidator validator) {
        logger.trace("Validating dummy patient");
        final Patient patient = new Patient();
        patient.addName().addGiven("Dummy").setFamily("Patient");
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(Date.valueOf("1990-01-01"));

        final ValidationOptions op = new ValidationOptions();
        op.addProfile(PatientProfile.PROFILE_URI);
        validator.validateWithResult(patient, op);
    }
}