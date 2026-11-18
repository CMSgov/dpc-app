package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.r4.OrganizationProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static gov.cms.dpc.testing.factories.r4.OrganizationFactory.generateFakeAddress;
import static gov.cms.dpc.testing.factories.r4.OrganizationFactory.generateFakeOrganization;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
class OrganizationValidationTest {

    private static FhirValidator fhirValidator;

    @BeforeAll
    static void setup() {
        FhirContext ctx = FhirContext.forR4();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
        fhirValidator.registerValidatorModule(instanceValidator);

        DPCProfileSupport dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void testIdentifier() {
        final Organization organization = generateFakeOrganization();
        organization.addAddress(generateFakeAddress());

        final ValidationResult result = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertFalse(result.isSuccessful(), "Should require an NPI identifier");

        organization.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi-value");
        final ValidationResult r2 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertFalse(r2.isSuccessful(), "An MBI should not satisfy the NPI requirement");

        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-value");
        final ValidationResult r3 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertTrue(r3.isSuccessful(), () -> r3.getMessages().toString());
    }

    @Test
    void testAddress() {
        final Organization organization = generateFakeOrganization();
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-value");

        final ValidationResult result = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertFalse(result.isSuccessful(), "Should require an address");

        organization.addAddress().setText("7500 Security Blvd").setType(Address.AddressType.PHYSICAL).setUse(Address.AddressUse.HOME);
        final ValidationResult r2 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertFalse(r2.isSuccessful(), "Should require the address fields in the DPC address profile");

        organization.setAddress(Collections.singletonList(generateFakeAddress()));
        final ValidationResult r3 = fhirValidator.validateWithResult(organization, new ValidationOptions().addProfile(OrganizationProfile.PROFILE_URI));
        assertTrue(r3.isSuccessful(), () -> r3.getMessages().toString());
    }
}
