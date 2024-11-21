package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.OrganizationProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static gov.cms.dpc.testing.factories.OrganizationFactory.generateFakeAddress;
import static gov.cms.dpc.testing.factories.OrganizationFactory.generateFakeOrganization;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Organization validation")
class OrganizationValidationTest {

    private static FhirValidator fhirValidator;
    private static DPCProfileSupport dpcModule;
    private static FhirContext ctx;

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);


        dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    @DisplayName("Validate organization with identifiers 🤮")
    void testIdentifier() {
        final Organization organization = generateFakeOrganization();
        organization.addAddress(generateFakeAddress());

        final ValidationResult result = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        organization.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi-value");

        final ValidationResult r2 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(2, r2.getMessages().size(), "Should have two failures for ID"));
    }

    @Test
    @DisplayName("Validate organization with identifiers 🥳")
    void testValidIdentifier() {
        final Organization organization = generateFakeOrganization();
        organization.addAddress(generateFakeAddress());
        organization.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi-value");
        // Add correct NPI
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-value");

        final ValidationResult r3 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertTrue(r3.isSuccessful(), "Should have passed");
    }

    @Test
    @DisplayName("Validate organization with address 🤮")
    void testAddress() {
        final Organization organization = generateFakeOrganization();
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-value");

        final ValidationResult result = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        // Add a text based Address
        organization.addAddress().setText("7500 Security Blvd").setType(Address.AddressType.PHYSICAL).setUse(Address.AddressUse.HOME);

        final ValidationResult r2 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(7, r2.getMessages().size(), "Should have multiple address failures"));
    }

    @Test
    @DisplayName("Validate organization with address 🥳")
    void testValidAddress() {
        final Organization organization = generateFakeOrganization();
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-value");
        // Add a text based Address
        organization.addAddress().setText("7500 Security Blvd").setType(Address.AddressType.PHYSICAL).setUse(Address.AddressUse.HOME);
        // Add valid address
        organization.setAddress(Collections.singletonList(generateFakeAddress()));

        final ValidationResult r3 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertTrue(r3.isSuccessful(), "Should have passed");
    }
}
