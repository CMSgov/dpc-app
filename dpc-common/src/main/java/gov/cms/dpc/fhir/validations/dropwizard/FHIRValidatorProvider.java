package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.sql.Date;

import static gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;

public class FHIRValidatorProvider implements Provider<FhirValidator> {

    private static final Logger logger = LoggerFactory.getLogger(FHIRValidatorProvider.class);

    private final FhirContext ctx;
    private final DPCProfileSupport dpcModule;
    private final FHIRValidationConfiguration validationConfiguration;

    @Inject
    public FHIRValidatorProvider(FhirContext ctx, DPCProfileSupport dpcModule, FHIRValidationConfiguration config) {
        this.ctx = ctx;
        this.dpcModule = dpcModule;
        this.validationConfiguration = config;
    }


    @Override
    public FhirValidator get() {
        logger.debug("Schema validation enabled: {}.\nSchematron validation enabled: {}", validationConfiguration.isSchemaValidation(), validationConfiguration.isSchematronValidation());
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();
        final FhirValidator fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(validationConfiguration.isSchematronValidation());
        fhirValidator.setValidateAgainstStandardSchema(validationConfiguration.isSchemaValidation());
        fhirValidator.registerValidatorModule(instanceValidator);

        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);
        initialize(fhirValidator);
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
