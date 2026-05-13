package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration.FHIRValidationConfiguration;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import io.dropwizard.jersey.validation.Validators;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;


/**
 * Guice module for setting up the required Validation components, if requested by the application
 */
public class FHIRValidationModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(FHIRValidationModule.class);

    private final FHIRValidationConfiguration config;

    public FHIRValidationModule(FHIRValidationConfiguration config) {
        this.config = config;
    }


    @Override
    protected void configure() {

        // Create a multi-binder for automatically bundling and injecting a Set of ConstraintValidators
        TypeLiteral<ConstraintValidator<?, ?>> constraintType = new TypeLiteral<>() {};
        Multibinder<ConstraintValidator<?, ?>> constraintBinder = Multibinder.newSetBinder(binder(), constraintType);
        constraintBinder.addBinding().to(ProfileValidator.class);

        bind(ConstraintValidatorFactory.class).to(InjectingConstraintValidatorFactory.class);
        bind(ConfiguredValidator.class).to(InjectingConfiguredValidator.class);

        bind(DPCProfileSupport.class).in(Scopes.SINGLETON);
    }

    @Provides
    FHIRValidationConfiguration provideValidationConfig() {
        return this.config;
    }

    @Provides
    Validator provideValidator(ValidatorFactory factory) {
        return factory.getValidator();
    }

    @Provides
    @Singleton
    ValidatorFactory provideValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        return Validators.newConfiguration()
            .constraintValidatorFactory(constraintValidatorFactory)
            .buildValidatorFactory();
    }

    @Provides
    ValidationSupportChain provideSupportChain(DPCProfileSupport dpcModule) {
        FhirContext ctx = FhirContext.forDstu3();
        return new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx));
    }

    @Provides
    @Singleton
    public FhirValidator provideFhirValidator(FhirContext ctx,
                                              FHIRValidationConfiguration validationConfig,
                                              ValidationSupportChain supportChain) {
        final FhirValidator fhirValidator = ctx.newValidator();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(supportChain);

        // Turn off the old validators.  They were failing things for field order, which shouldn't matter.
        fhirValidator.setValidateAgainstStandardSchema(validationConfig.isSchemaValidation());
        fhirValidator.setValidateAgainstStandardSchematron(validationConfig.isSchematronValidation());

        fhirValidator.registerValidatorModule(instanceValidator);
        primeValidator(fhirValidator);

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
    private void primeValidator(FhirValidator validator) {
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
